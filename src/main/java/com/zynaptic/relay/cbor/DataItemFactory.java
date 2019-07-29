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

package com.zynaptic.relay.cbor;

import java.util.List;
import java.util.Map;

/**
 * Provides the CBOR data item factory interface, which may be used for
 * programmatically generating CBOR messages.
 *
 * @author Chris Holgate
 */
public interface DataItemFactory {

  /**
   * Creates a new invalid data item with the appropriate failed decoding status
   * value.
   *
   * @param decodeStatus This is the decoding status value which is associated
   *   with the invalid data item. The selected decoding status value must
   *   correspond to a failure condition.
   * @return Returns a new invalid data item which will report the appropriate
   *   failed decoding status value via the {@link DataItem#getDecodeStatus()}
   *   method.
   */
  public DataItem<?> createInvalidItem(DecodeStatus decodeStatus);

  /**
   * Creates a new boolean data item for subsequent inclusion in a CBOR formatted
   * message.
   *
   * @param value This is the boolean value which is associated with the boolean
   *   data item.
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created boolean data item.
   */
  public DataItem<Boolean> createBooleanItem(boolean value, int[] tags);

  /**
   * Creates a new undefined data item for subsequent inclusion in a CBOR
   * formatted message.
   *
   * @param isNull This is a boolean value which when set to 'true' indicates that
   *   the undefined data is to be characterised as a null data item of type
   *   {@link UserDataType#NULL}. Otherwise it is classified as an undefined data
   *   item of type {@link UserDatatype#UNDEFINED}.
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created simple data item.
   */
  public DataItem<Boolean> createUndefinedItem(boolean isNull, int[] tags);

  /**
   * Creates a new simple data item for subsequent inclusion in a CBOR formatted
   * message.
   *
   * @param value This is the simple data value which is associated with the
   *   simple data item. It must be one of the unassigned values specified in
   *   RFC7049 section 2.3.
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created simple data item.
   */
  public DataItem<Integer> createSimpleDataItem(int value, int[] tags);

  /**
   * Creates a new integer data item for subsequent inclusion in a CBOR formatted
   * message. Integer values are restricted by the implementation to those that
   * can be represented using 64-bit 2's complement notation (ie, Java long
   * integers).
   *
   * @param value This is the integer value which is associated with the integer
   *   data item.
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created integer data item.
   */
  public DataItem<Long> createIntegerItem(long value, int[] tags);

  /**
   * Creates a new half precision floating point data item for subsequent
   * inclusion in a CBOR formatted message.
   *
   * @param value This is the 32-bit single precision floating point value which
   *   is to be converted to half precision format. This involves rounding the
   *   mantissa to account for the reduced precision and addressing the reduced
   *   exponent range by mapping absolute values above the 65504 upper limit to
   *   infinity.
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created single precision floating point data item.
   */
  public DataItem<Float> createHpFloatItem(float value, int[] tags);

  /**
   * Creates a new single precision floating point data item for subsequent
   * inclusion in a CBOR formatted message.
   *
   * @param value This is the 32-bit single precision floating point value which
   *   is associated with the floating point data item.
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created single precision floating point data item.
   */
  public DataItem<Float> createSpFloatItem(float value, int[] tags);

  /**
   * Creates a new double precision floating point data item for subsequent
   * inclusion in a CBOR formatted message.
   *
   * @param value This is the 64-bit double precision floating point value which
   *   is associated with the floating point data item.
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created double precision floating point data item.
   */
  public DataItem<Double> createDpFloatItem(double value, int[] tags);

  /**
   * Creates a new fixed length text string data item for subsequent inclusion as
   * a UTF-8 formatted text string in a CBOR message.
   *
   * @param textString This is the text string which is associated with the text
   *   string data item.
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created fixed length text string data item.
   */
  public DataItem<String> createTextStringItem(String textString, int[] tags);

  /**
   * Creates a new indefinite length text string data item for subsequent
   * inclusion as a list of UTF-8 formatted substrings in a CBOR message. The data
   * item is created as an empty list, which can then be modified by adding the
   * required substrings.
   *
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created indefinite length text string data item. This
   *   will be a mutable data item which allows the encapsulated list to be
   *   modified.
   */
  public DataItem<List<String>> createTextStringListItem(int[] tags);

  /**
   * Creates a new fixed length byte string data item for subsequent inclusion as
   * a byte string in a CBOR formatted message.
   *
   * @param byteString This is the byte string which is associated with the byte
   *   string data item.
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created fixed length byte string data item.
   */
  public DataItem<byte[]> createByteStringItem(byte[] byteString, int[] tags);

  /**
   * Creates a new fixed length byte string data item for subsequent inclusion as
   * a byte string in a CBOR formatted message, using a base64 encoded string as
   * the data source.
   *
   * @param encodedString This is a base64 encoded string which will be decoded to
   *   yield the byte array which is associated with the byte string data item.
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created fixed length byte string data item.
   */
  public DataItem<byte[]> createByteStringItem(String encodedString, int[] tags);

  /**
   * Creates a new indefinite length byte string data item for subsequent
   * inclusion as a list of substrings in a CBOR message. The data item is created
   * as an empty list, which can then be modified by adding the required
   * substrings.
   *
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @return Returns a newly created indefinite length byte string data item. This
   *   will be a mutable data item which allows the encapsulated list to be
   *   modified.
   */
  public DataItem<List<byte[]>> createByteStringListItem(int[] tags);

  /**
   * Creates a new array data item for subsequent inclusion as an array in a CBOR
   * message. The data item is created as an empty list, which can then be
   * modified by adding the required array entries.
   *
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @param indefiniteLength This is a boolean flag which specifies the CBOR array
   *   format to be used. This will be indefinite length encoding if set to 'true'
   *   and fixed length encoding otherwise.
   * @return Returns a newly created array data item. This will be a mutable data
   *   item which allows the encapsulated list to be modified.
   */
  public DataItem<List<DataItem<?>>> createArrayItem(int[] tags, boolean indefiniteLength);

  /**
   * Creates a new indexed map for subsequent inclusion as a map in a CBOR
   * message. The data item is created as an empty map, which can then be modified
   * by adding the required map entries. This type of map will always use long
   * integer key values.
   *
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @param indefiniteLength This is a boolean flag which specifies the CBOR map
   *   format to be used. This will be indefinite length encoding if set to 'true'
   *   and fixed length encoding otherwise.
   * @return Returns a newly created indexed map data item. This will be a mutable
   *   data item which allows the encapsulated map to be modified.
   */
  public DataItem<Map<Long, DataItem<?>>> createIndexedMapItem(int[] tags, boolean indefiniteLength);

  /**
   * Creates a new named map for subsequent inclusion as a map in a CBOR message.
   * The data item is created as an empty map, which can then be modified by
   * adding the required map entries. This type of map will always use text string
   * key values.
   *
   * @param tags This is a list of data item tags which will be prepended to the
   *   data item during CBOR encoding.
   * @param indefiniteLength This is a boolean flag which specifies the CBOR map
   *   format to be used. This will be indefinite length encoding if set to 'true'
   *   and fixed length encoding otherwise.
   * @return Returns a newly created named map data item. This will be a mutable
   *   data item which allows the encapsulated map to be modified.
   */
  public DataItem<Map<String, DataItem<?>>> createNamedMapItem(int[] tags, boolean indefiniteLength);

}
