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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.ExtensionType;
import com.zynaptic.relay.cbor.MajorType;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides the base class implementation of the CBOR data item interface. This
 * is subclassed in order to implement the various concrete types of CBOR data
 * item object.
 *
 * @param <T> This is the generic runtime data type which is associated with a
 *   given CBOR data item type.
 *
 * @author Chris Holgate
 */
abstract class DataItemCore<T> implements DataItem<T> {

  // Specifies the byte encoding for the CBOR indefinite length break code.
  static final int BREAK_STOP_CODE = 0xFF;

  // Specifies the byte encoding for the CBOR indefinite length indicator.
  private static final int INDEFINITE_LENGTH_CODE = 0x1F;

  // Identifies the user data type for the data item.
  private final UserDataType dataType;

  // Stores the list of tag values associated with the data item.
  private final int[] tags;

  // Specifies whether the associated data is mutable (ie, array, map and list
  // data items can be modified by the application).
  private final boolean mutable;

  // Specifies whether indefinite length encoding was used by decoded CBOR data
  // or will be used during encoding.
  private final boolean indefiniteLength;

  // Specifies the decoding status which is associated with the data item.
  private DecodeStatus decodeStatus;

  /**
   * Provides superclass constructor which is used for setting up common data
   * fields for all data item types.
   *
   * @param dataType This is the user data type which can be used to determine the
   *   runtime type of the associated data.
   * @param tags This is a list of tag values which are to be associated with the
   *   data item.
   * @param mutable This is a boolean flag which will be used to indicate whether
   *   the associated data is mutable.
   * @param indefiniteLength This is a boolean flag which will be used to indicate
   *   whether indefinite length encoding was used by decoded CBOR data or will be
   *   used during encoding.
   */
  DataItemCore(final UserDataType dataType, final int[] tags, final boolean mutable, final boolean indefiniteLength) {
    this.dataType = dataType;
    this.tags = tags;
    this.mutable = mutable;
    this.indefiniteLength = indefiniteLength;
    this.decodeStatus = DecodeStatus.ORIGINAL;
  }

  /*
   * Implements DataItem.getDataType()
   */
  @Override
  public final UserDataType getDataType() {
    return dataType;
  }

  /*
   * Implements DataItem.getTags()
   */
  @Override
  public final int[] getTags() {
    return (tags == null) ? null : tags.clone();
  }

  /*
   * Implements DataItem.isMutable()
   */
  @Override
  public final boolean isMutable() {
    return mutable;
  }

  /*
   * Implements DataItem.isIndefiniteLength()
   */
  @Override
  public final boolean isIndefiniteLength() {
    return indefiniteLength;
  }

  /*
   * Implements DataItem.castData()
   */
  @Override
  @SuppressWarnings("unchecked")
  public final <U> U castData() {
    return (U) getData();
  }

  /*
   * Implements DataItem.setDecodeStatus(...)
   */
  @Override
  public final DataItem<T> setDecodeStatus(final DecodeStatus newDecodeStatus) throws IllegalStateException {
    if (decodeStatus.isFailure()) {
      if (!newDecodeStatus.isFailure()) {
        throw new IllegalStateException("Decode status has previously been assigned to a failure condition.");
      }
    }
    decodeStatus = newDecodeStatus;
    return this;
  }

  /*
   * Implements DataItem.getDecodeStatus()
   */
  @Override
  public final DecodeStatus getDecodeStatus() {
    return decodeStatus;
  }

  /**
   * Writes the primary data field for a given extension data type.
   *
   * @param extensionType This is the CBOR extension type for which the primary
   *   data field is being generated.
   * @param outputStream This is the output data stream to which the primary data
   *   field is being written.
   * @param tags This is the set of integer tag values which are to be prepended
   *   to the primary data field.
   * @throws IOException This exception will be thrown on failure to write the
   *   primary data field.
   */
  void writeExtensionPrimaryData(final ExtensionType extensionType, final DataOutputStream outputStream,
      final int[] tags) throws IOException {

    // Prepend the data item with the specified tags (if present). These are
    // written in order, so the rightmost tag in the list binds most closely
    // with the primary data.
    if (tags != null) {
      for (int i = 0; i < tags.length; i++) {
        writePrimaryData(MajorType.TAG, tags[i], outputStream, null);
      }
    }

    // Write the single byte primary data value which encodes the extension data
    // type.
    outputStream.writeByte(MajorType.EXTENSION.getByteEncoding(extensionType.getByteEncoding()));
  }

  /**
   * Writes the primary data field for a given major data type which uses
   * indefinite length formatting.
   *
   * @param majorType This is the CBOR major data type for which the primary data
   *   field is being generated.
   * @param outputStream This is the output data stream to which the primary data
   *   field is being written.
   * @param tags This is the set of integer tag values which are to be prepended
   *   to the primary data field.
   * @throws IOException This exception will be thrown on failure to write the
   *   primary data field.
   */
  void writeIndefinitePrimaryData(final MajorType majorType, final DataOutputStream outputStream, final int[] tags)
      throws IOException {

    // Prepend the data item with the specified tags (if present). These are
    // written in order, so the rightmost tag in the list binds most closely
    // with the primary data.
    if (tags != null) {
      for (int i = 0; i < tags.length; i++) {
        writePrimaryData(MajorType.TAG, tags[i], outputStream, null);
      }
    }

    // Write the single byte primary data value which encodes the indefinite
    // length item.
    outputStream.writeByte(majorType.getByteEncoding(INDEFINITE_LENGTH_CODE));
  }

  /**
   * Writes the primary data field for a given major data type which includes
   * variable length integer value encoding.
   *
   * @param majorType This is the CBOR major data type for which the primary data
   *   field is being generated.
   * @param primaryData This is the integer value which is to be encoded in the
   *   primary data field.
   * @param outputStream This is the output data stream to which the primary data
   *   field is being written.
   * @param tags This is the set of integer tag values which are to be prepended
   *   to the primary data field.
   * @throws IOException This exception will be thrown on failure to write the
   *   primary data field.
   */
  void writePrimaryData(final MajorType majorType, final long primaryData, final DataOutputStream outputStream,
      final int[] tags) throws IOException {

    // Prepend the data item with the specified tags (if present). These are
    // written in order, so the rightmost tag in the list binds most closely
    // with the primary data.
    if (tags != null) {
      for (int i = 0; i < tags.length; i++) {
        writePrimaryData(MajorType.TAG, tags[i], outputStream, null);
      }
    }

    // Writes negative values using the full 8-byte representation. These
    // correspond to the upper half of the conventional 64-bit unsigned integer
    // range (ie, 2^63 to 2^64-1).
    if (primaryData < 0) {
      final int initialByte = majorType.getByteEncoding(27);
      outputStream.writeByte(initialByte);
      outputStream.writeLong(primaryData);
    }

    // Writes small unsigned values using compact single byte notation.
    else if (primaryData < 24) {
      final int initialByte = majorType.getByteEncoding((int) primaryData);
      outputStream.writeByte(initialByte);
    }

    // Write full scale unsigned byte values.
    else if (primaryData < 0x100) {
      final int initialByte = majorType.getByteEncoding(24);
      outputStream.writeByte(initialByte);
      outputStream.writeByte((int) primaryData);
    }

    // Write full scale 16-bit unsigned short values.
    else if (primaryData < 0x10000) {
      final int initialByte = majorType.getByteEncoding(25);
      outputStream.writeByte(initialByte);
      outputStream.writeShort((int) primaryData);
    }

    // Write full scale 32-bit unsigned integer values.
    else if (primaryData < 0x100000000L) {
      final int initialByte = majorType.getByteEncoding(26);
      outputStream.writeByte(initialByte);
      outputStream.writeInt((int) primaryData);
    }

    // Writes 64-bit positive range integer values (ie, up to 2^63-1).
    else {
      final int initialByte = majorType.getByteEncoding(27);
      outputStream.writeByte(initialByte);
      outputStream.writeLong(primaryData);
    }
  }

  /**
   * Appends the CBOR representation for a given data item to the supplied data
   * output stream. Note that the primitive type writer methods used in the data
   * output stream class are big endian, which makes them consistent with network
   * byte order.
   *
   * @param outputStream This is the data output stream to which the encoded CBOR
   *   data item is to be written.
   * @throws IOException This exception will be thrown on failure to write the
   *   encoded CBOR data item.
   */
  abstract void appendCbor(DataOutputStream outputStream) throws IOException;

  /**
   * Appends the JSON representation for a given data item to the supplied print
   * output stream.
   *
   * @param printWriter This is the print output writer to which the JSON encoded
   *   CBOR data item is to be written.
   * @param indent This specifies the level of indenting to be applied when
   *   formatting the JSON text. A negative value implies that no formatting
   *   should be applied.
   */
  abstract void appendJson(PrintWriter printWriter, int indent);

}
