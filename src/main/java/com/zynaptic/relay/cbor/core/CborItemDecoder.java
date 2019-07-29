/*
 * Zynaptic Relay CBOR library - An RFC7049 based data serialisation library.
 *
 * Copyright (c) 2015-2019, Zynaptic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Please visit www.zynaptic.com or contact reaction@zynaptic.com if you need
 * additional information or have any questions.
 */

package com.zynaptic.relay.cbor.core;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.ExtensionType;
import com.zynaptic.relay.cbor.MajorType;

/**
 * Provides CBOR data item decoding capabilities. The data item decoder may be
 * used to parse a single data item per invocation, returning the decoding state
 * and providing subsequent access to the decoded data item.
 *
 * @author Chris Holgate
 */
final class CborItemDecoder {

  // Specifies the byte encoding for the CBOR indefinite length break code.
  private static final int BREAK_STOP_CODE = 0xFF;

  // Specifies the data input stream to be used by the decoder.
  private final DataInputStream inputStream;

  /**
   * Default constructor assigns the decoder to a given data input stream.
   *
   * @param inputStream This is the data input stream with which the decoder is
   *   associated.
   */
  CborItemDecoder(final DataInputStream inputStream) {
    this.inputStream = inputStream;
  }

  /**
   * Performs a single data item decoding step. This processes the next data item
   * in the input data stream, returning the decoding status on completion.
   *
   * @return Returns the decoded data item which also encapsulates the decoding
   *   status that reflects the success or failure of the decoding operation.
   * @throws IOException This exception will be thrown if an I/O error prevented
   *   the input data stream from being decoded.
   */
  DataItem<?> decode() throws IOException {
    try {
      return recursiveDecode(inputStream.readUnsignedByte());
    } catch (final EOFException error) {
      error.printStackTrace();
      return UndefinedDataItem.INVALID;
    }
  }

  /*
   * Performs single data item decoding step after the initial data item byte has
   * been read from the input stream.
   */
  private DataItem<?> recursiveDecode(int initialByte) throws IOException {
    Long primaryData;
    int[] tags = null;

    // Process leading tags if present. This procedure imposes an implementation
    // dependent limit on the range of tag values which is 0 to 2^31-1.
    MajorType majorType = MajorType.getMajorType((byte) initialByte);
    while (majorType == MajorType.TAG) {
      if ((primaryData = readPrimaryData(initialByte)) == null) {
        return UndefinedDataItem.INVALID;
      } else if ((primaryData < 0) || (primaryData > Integer.MAX_VALUE)) {
        return UndefinedDataItem.UNSUPPORTED;
      }

      // Build the array of tag values. The common case is that only one tag is
      // present per data item, so the code is optimised for this situation.
      if (tags == null) {
        tags = new int[] { primaryData.intValue() };
      } else {
        tags = Arrays.copyOf(tags, tags.length + 1);
        tags[tags.length - 1] = primaryData.intValue();
      }

      // Process the initial byte of the next data item.
      initialByte = inputStream.readUnsignedByte();
      majorType = MajorType.getMajorType((byte) initialByte);
    }

    // Read the primary data value and then select the major data type to be
    // processed.
    DataItem<?> dataItem;
    primaryData = readPrimaryData(initialByte);
    switch (majorType) {

    // Generate integer data items from unsigned and negative major data types.
    case UNSIGNED:
    case NEGATIVE:
      dataItem = processInteger(majorType, primaryData, tags);
      break;

    // Generate byte and text strings from either fixed or indefinite length
    // byte arrays.
    case BYTE_STRING:
    case TEXT_STRING:
      if (isIndefiniteLength(initialByte)) {
        dataItem = processIndefiniteLengthString(majorType, tags);
      } else {
        dataItem = processFixedLengthString(majorType, primaryData, tags);
      }
      break;

    // Generate data item array from either fixed or indefinite length
    // representations.
    case ARRAY:
      if (isIndefiniteLength(initialByte)) {
        dataItem = processIndefiniteLengthArray(tags);
      } else {
        dataItem = processFixedLengthArray(primaryData, tags);
      }
      break;

    // Generate data item array from either fixed or indefinite length
    // representations.
    case MAP:
      if (isIndefiniteLength(initialByte)) {
        dataItem = processIndefiniteLengthMap(tags);
      } else {
        dataItem = processFixedLengthMap(primaryData, tags);
      }
      break;

    // Generate boolean, floating point and simple data items using the
    // extension data types.
    case EXTENSION:
      dataItem = processExtensionTypes(initialByte, primaryData, tags);
      break;
    default:
      System.out.println("Which major type?");
      dataItem = UndefinedDataItem.INVALID;
      break;
    }
    return dataItem;
  }

  /*
   * Processes the variable length primary data field for a given data item.
   * Depending on the major data type, this will represent either the actual data
   * value or the length of a subsequent map, array or string.
   */
  private Long readPrimaryData(final int initialByte) throws IOException {
    Long primaryData;
    final int additionalInfo = 0x1F & initialByte;
    switch (additionalInfo) {
    default:
      primaryData = (long) additionalInfo;
      break;
    case 24:
      primaryData = (long) inputStream.readUnsignedByte();
      break;
    case 25:
      primaryData = (long) inputStream.readUnsignedShort();
      break;
    case 26:
      primaryData = 0xFFFFFFFFL & inputStream.readInt();
      break;
    case 27:
      primaryData = inputStream.readLong();
      break;
    case 28:
    case 29:
    case 30:
    case 31:
      primaryData = null;
      break;
    }
    return primaryData;
  }

  /*
   * Determines whether a data item uses the indefinite length format, given the
   * initial byte value.
   */
  private boolean isIndefiniteLength(final int initialByte) {
    return ((0x1F & initialByte) == 31);
  }

  /*
   * Extracts an integer value from the primary data. Note that the full scale
   * range of integer values is restricted by this implementation to the 64-bit
   * 2's complement range.
   */
  private DataItem<?> processInteger(final MajorType majorType, final Long primaryData, final int[] tags) {
    if (primaryData == null) {
      return UndefinedDataItem.INVALID;
    }
    if (primaryData < 0) {
      return UndefinedDataItem.UNSUPPORTED;
    }

    // Process the primary data value as an integer. CBOR negative values use
    // 1's complement notation.
    final long value = (majorType == MajorType.NEGATIVE) ? ~primaryData : primaryData;
    return new IntegerDataItem(value, tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
  }

  /*
   * Extracts a fixed length byte string from the input stream. Note that the
   * length of the underlying byte array is restricted by this implementation to
   * the 32-bit 2's complement range.
   */
  private DataItem<?> processFixedLengthString(final MajorType majorType, final Long primaryData, final int[] tags)
      throws IOException {
    if (primaryData == null) {
      return UndefinedDataItem.INVALID;
    }
    if ((primaryData < 0) || (primaryData > Integer.MAX_VALUE)) {
      return UndefinedDataItem.UNSUPPORTED;
    }

    // Read in the fixed byte string and create the appropriate data item.
    final byte[] byteString = new byte[primaryData.intValue()];
    inputStream.readFully(byteString);

    // Builds either a raw byte string or a text string, depending on the
    // specified major type.
    if (majorType == MajorType.BYTE_STRING) {
      return new ByteStringDataItem(byteString, tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
    } else {
      final String textString = new String(byteString, "UTF-8");
      return new TextStringDataItem(textString, tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
    }
  }

  /*
   * Extracts an indefinite length byte string from the input stream. Note that
   * the total number and length of all the underlying byte arrays is restricted
   * by this implementation to the 32-bit 2's complement range.
   */
  private DataItem<?> processIndefiniteLengthString(final MajorType majorType, final int[] tags) throws IOException {
    final List<byte[]> segmentList = new LinkedList<byte[]>();
    int initialByte;

    // Iterate over enclosed data items until reaching the break stop code byte.
    // All enclosed data items must be fixed length byte strings.
    while ((initialByte = inputStream.readUnsignedByte()) != BREAK_STOP_CODE) {
      if (segmentList.size() == Integer.MAX_VALUE) {
        return UndefinedDataItem.UNSUPPORTED;
      }
      final MajorType segmentType = MajorType.getMajorType((byte) initialByte);
      final Long primaryData = readPrimaryData(initialByte);
      if ((segmentType != majorType) || (primaryData == null)) {
        return UndefinedDataItem.INVALID;
      }
      if ((primaryData < 0) || (primaryData > Integer.MAX_VALUE)) {
        return UndefinedDataItem.UNSUPPORTED;
      }

      // Read in the fixed byte string segment and append to the segment list.
      final byte[] byteStringSegment = new byte[primaryData.intValue()];
      inputStream.readFully(byteStringSegment);
      segmentList.add(byteStringSegment);
    }

    // Builds either a raw byte string list data item or a text string list data
    // item, depending on the specified major type.
    if (majorType == MajorType.BYTE_STRING) {
      return new ByteStringListDataItem(segmentList, tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
    } else {
      final List<String> stringList = new ArrayList<String>(segmentList.size());
      for (final byte[] byteString : segmentList) {
        stringList.add(new String(byteString, "UTF-8"));
      }
      return new TextStringListDataItem(stringList, tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
    }
  }

  /*
   * Extracts a fixed length array of data items. Note that the length of the
   * array is restricted by this implementation to the 32-bit 2's complement
   * range.
   */
  private DataItem<?> processFixedLengthArray(final Long primaryData, final int[] tags) throws IOException {
    DecodeStatus decodeStatus = DecodeStatus.TRANSLATABLE;
    if (primaryData == null) {
      return UndefinedDataItem.INVALID;
    }
    if ((primaryData < 0) || (primaryData > Integer.MAX_VALUE)) {
      return UndefinedDataItem.UNSUPPORTED;
    }

    System.out.println("Processing fixed length array.");

    // Iterate over the required number of data items, adding them to the data
    // item list. Note that we take into account the least strict decoding
    // criteria of all the data items.
    final List<DataItem<?>> dataItemList = new ArrayList<DataItem<?>>(primaryData.intValue());
    for (int i = 0; i < primaryData.intValue(); i++) {
      final DataItem<?> nextDataItem = recursiveDecode(inputStream.readUnsignedByte());
      decodeStatus = decodeStatus.getLeastStrictCriteria(nextDataItem.getDecodeStatus());
      if (decodeStatus.isFailure()) {
        return new UndefinedDataItem(decodeStatus);
      }
      dataItemList.add(nextDataItem);
    }

    // Build the immutable array data item.
    return new ArrayDataItem(dataItemList, tags, false).setDecodeStatus(decodeStatus);
  }

  /*
   * Extracts a variable length array of data items. Note that the length of the
   * array is restricted by this implementation to the 32-bit 2's complement
   * range.
   */
  private DataItem<?> processIndefiniteLengthArray(final int[] tags) throws IOException {
    DecodeStatus decodeStatus = DecodeStatus.TRANSLATABLE;
    int initialByte;

    System.out.println("Processing indefinite length array.");

    // Iterate over the indefinite number of data items, adding them to the data
    // item list. Note that we take into account the least strict decoding
    // criteria of all the data items.
    final List<DataItem<?>> dataItemList = new LinkedList<DataItem<?>>();
    while ((initialByte = inputStream.readUnsignedByte()) != BREAK_STOP_CODE) {
      if (dataItemList.size() == Integer.MAX_VALUE) {
        return UndefinedDataItem.UNSUPPORTED;
      }
      final DataItem<?> nextDataItem = recursiveDecode(initialByte);
      decodeStatus = decodeStatus.getLeastStrictCriteria(nextDataItem.getDecodeStatus());
      System.out.println("Init byte: " + initialByte + " Status: " + decodeStatus);
      if (decodeStatus.isFailure()) {
        return new UndefinedDataItem(decodeStatus);
      }
      dataItemList.add(nextDataItem);
    }

    // Build the immutable array data item.
    return new ArrayDataItem(dataItemList, tags, true).setDecodeStatus(decodeStatus);
  }

  /*
   * Extracts a fixed length map of data items. Note that the size of the map is
   * restricted by this implementation to the 32-bit 2's complement range.
   */
  private DataItem<?> processFixedLengthMap(final Long primaryData, final int[] tags) throws IOException {
    DecodeStatus decodeStatus = DecodeStatus.TRANSLATABLE;
    if (primaryData == null) {
      return UndefinedDataItem.INVALID;
    }
    if ((primaryData < 0) || (primaryData > Integer.MAX_VALUE)) {
      return UndefinedDataItem.UNSUPPORTED;
    }

    // Iterate over the required number of data item pairs, adding them to the
    // data item map. Note that we take into account the least strict decoding
    // criteria of all the data items.
    Map<String, DataItem<?>> namedMap = null;
    Map<Long, DataItem<?>> indexedMap = null;
    for (int i = 0; i < primaryData.intValue(); i++) {

      // Extract the CBOR key item.
      final DataItem<?> keyItem = recursiveDecode(inputStream.readUnsignedByte());
      decodeStatus = decodeStatus.getLeastStrictCriteria(keyItem.getDecodeStatus());
      if (decodeStatus.isFailure()) {
        return new UndefinedDataItem(decodeStatus);
      }

      // Create a map of the appropriate type on extracting the first key.
      if ((namedMap == null) && (indexedMap == null)) {
        if (keyItem instanceof TextStringDataItem) {
          namedMap = new HashMap<String, DataItem<?>>();
        } else if (keyItem instanceof IntegerDataItem) {
          indexedMap = new HashMap<Long, DataItem<?>>();
        } else {
          return UndefinedDataItem.UNSUPPORTED;
        }
      }

      // Extract the CBOR value item.
      final DataItem<?> valueItem = recursiveDecode(inputStream.readUnsignedByte());
      decodeStatus = decodeStatus.getLeastStrictCriteria(valueItem.getDecodeStatus());
      if (decodeStatus.isFailure()) {
        return new UndefinedDataItem(decodeStatus);
      }

      // Insert the new entry into the named map if appropriate, checking for
      // duplicates.
      if (namedMap != null) {
        if (!(keyItem instanceof TextStringDataItem)) {
          return UndefinedDataItem.UNSUPPORTED;
        } else if (namedMap.containsKey(keyItem.getData())) {
          decodeStatus = decodeStatus.getLeastStrictCriteria(DecodeStatus.WELL_FORMED);
        } else {
          namedMap.put((String) keyItem.getData(), valueItem);
        }
      }

      // Insert the new entry into the indexed map if appropriate, checking for
      // duplicates.
      else if (indexedMap != null) {
        if (!(keyItem instanceof IntegerDataItem)) {
          return UndefinedDataItem.UNSUPPORTED;
        } else if (indexedMap.containsKey(keyItem.getData())) {
          decodeStatus = decodeStatus.getLeastStrictCriteria(DecodeStatus.WELL_FORMED);
        } else {
          indexedMap.put((Long) keyItem.getData(), valueItem);
        }
      }
    }

    // Build the immutable map data item. Note that if the map is empty, a named
    // map data item is created with a null entry set.
    if (indexedMap != null) {
      return new IndexedMapDataItem(indexedMap, tags, false).setDecodeStatus(decodeStatus);
    } else {
      return new NamedMapDataItem(namedMap, tags, false).setDecodeStatus(decodeStatus);
    }
  }

  /*
   * Extracts a variable length map of data items. Note that the size of the map
   * is restricted by this implementation to the 32-bit 2's complement range.
   */
  private DataItem<?> processIndefiniteLengthMap(final int[] tags) throws IOException {
    DecodeStatus decodeStatus = DecodeStatus.TRANSLATABLE;
    int initialByte;

    // Iterate over the indefinite number of data items, adding them to the data
    // item list. Note that we take into account the least strict decoding
    // criteria of all the data items.
    Map<String, DataItem<?>> namedMap = null;
    Map<Long, DataItem<?>> indexedMap = null;
    while ((initialByte = inputStream.readUnsignedByte()) != BREAK_STOP_CODE) {

      // Extract the CBOR key item.
      final DataItem<?> keyItem = recursiveDecode(initialByte);
      decodeStatus = decodeStatus.getLeastStrictCriteria(keyItem.getDecodeStatus());
      if (decodeStatus.isFailure()) {
        return new UndefinedDataItem(decodeStatus);
      }

      // Create a map of the appropriate type on extracting the first key.
      if ((namedMap == null) && (indexedMap == null)) {
        if (keyItem instanceof TextStringDataItem) {
          namedMap = new HashMap<String, DataItem<?>>();
        } else if (keyItem instanceof IntegerDataItem) {
          indexedMap = new HashMap<Long, DataItem<?>>();
        } else {
          return UndefinedDataItem.UNSUPPORTED;
        }
      }

      // Extract the CBOR value item.
      final DataItem<?> valueItem = recursiveDecode(inputStream.readUnsignedByte());
      decodeStatus = decodeStatus.getLeastStrictCriteria(valueItem.getDecodeStatus());
      if (decodeStatus.isFailure()) {
        return new UndefinedDataItem(decodeStatus);
      }

      // Insert the new entry into the named map if appropriate, checking for
      // duplicates.
      if (namedMap != null) {
        if (!(keyItem instanceof TextStringDataItem)) {
          return UndefinedDataItem.UNSUPPORTED;
        } else if (namedMap.containsKey(keyItem.getData())) {
          decodeStatus = decodeStatus.getLeastStrictCriteria(DecodeStatus.WELL_FORMED);
        } else {
          namedMap.put((String) keyItem.getData(), valueItem);
        }
      }

      // Insert the new entry into the indexed map if appropriate, checking for
      // duplicates.
      else if (indexedMap != null) {
        if (!(keyItem instanceof IntegerDataItem)) {
          return UndefinedDataItem.UNSUPPORTED;
        } else if (indexedMap.containsKey(keyItem.getData())) {
          decodeStatus = decodeStatus.getLeastStrictCriteria(DecodeStatus.WELL_FORMED);
        } else {
          indexedMap.put((Long) keyItem.getData(), valueItem);
        }
      }
    }

    // Build the immutable map data item. Note that if the map is empty, a named
    // map data item is created with a zero entry set.
    if (indexedMap != null) {
      return new IndexedMapDataItem(indexedMap, tags, true).setDecodeStatus(decodeStatus);
    } else {
      return new NamedMapDataItem(namedMap, tags, true).setDecodeStatus(decodeStatus);
    }
  }

  /*
   * Extracts boolean, floating point and simple data values using the extension
   * data types.
   */
  private DataItem<?> processExtensionTypes(final int initialByte, final Long primaryData, final int[] tags) {
    DataItem<?> dataItem;
    final ExtensionType extensionType = ExtensionType.getExtensionType((byte) initialByte);
    if ((primaryData == null) || (extensionType == null)) {
      return UndefinedDataItem.INVALID;
    }
    switch (extensionType) {

    // Extract boolean data type values.
    case TRUE:
      dataItem = new BooleanDataItem(true, tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
      break;
    case FALSE:
      dataItem = new BooleanDataItem(false, tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
      break;

    // Extract undefined and null data values.
    case NULL:
      dataItem = new UndefinedDataItem(true, tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
      break;
    case UNDEFINED:
      dataItem = new UndefinedDataItem(false, tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
      break;

    // Extract floating point values.
    case FLOAT16:
      dataItem = new FloatHalfDataItem(primaryData.intValue(), tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
      break;
    case FLOAT32:
      dataItem = new FloatStandardDataItem(primaryData.intValue(), tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
      break;
    case FLOAT64:
      dataItem = new FloatDoubleDataItem(primaryData.longValue(), tags).setDecodeStatus(DecodeStatus.TRANSLATABLE);
      break;

    // Out of sequence break conditions indicate an invalid data format. All
    // remaining values are mapped to generic simple data items. These are
    // considered to be well formed, but cannot be translated into meaningful
    // JSON.
    case BREAK:
      dataItem = UndefinedDataItem.INVALID;
      break;
    default:
      dataItem = new SimpleDataItem(primaryData.intValue(), tags).setDecodeStatus(DecodeStatus.WELL_FORMED);
      break;
    }
    return dataItem;
  }
}
