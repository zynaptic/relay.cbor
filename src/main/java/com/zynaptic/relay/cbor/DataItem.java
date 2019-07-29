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

/**
 * Provides the public interface for accessing CBOR data item objects.
 *
 * @param <T> This is the Java native type which is associated with a given CBOR
 *   data item.
 *
 * @author Chris Holgate
 *
 */
public interface DataItem<T> {

  /**
   * Accesses the user data type which is associated with a CBOR data item. The
   * full set of user data types is specified using the {@link UserDataType}
   * enumeration.
   *
   * @return Returns the user data type which may be used to infer the type of
   *   data which will be returned by a call to {@link #getData}.
   */
  public UserDataType getDataType();

  /**
   * Accesses the data which is encapsulated by a CBOR data item. The type of the
   * returned data value can be inferred from the user data type, as accessed by
   * the {@link #getDataType} method.
   *
   * @return Returns the data which is encapsulated by a CBOR data item.
   */
  public T getData();

  /**
   * Accesses the data which is encapsulated by a CBOR data item using unchecked
   * casting. The type of the returned data value can be inferred from the user
   * data type, as accessed by the {@link #getDataType} method. This method is
   * useful in contexts where the data item type is parameterised with a wildcard
   * (for example CBOR map and array entries).
   *
   * @return Returns the data which is encapsulated by a CBOR data item, after an
   *   unchecked cast to the same type as the assignment target.
   */
  public <U> U castData();

  /**
   * Determines whether the data which is encapsulated by a CBOR data item is
   * mutable. Mutable maps, lists and arrays may be modified prior to CBOR
   * encoding. Immutable maps, lists and arrays correspond to decoded CBOR data
   * and cannot be modified.
   *
   * @return Returns a boolean value which will be set to 'true' if the
   *   encapsulated data is mutable.
   */
  public boolean isMutable();

  /**
   * Determines whether the data which is encapsulated by a CBOR data item uses
   * indefinite length encoding. During CBOR generation, this specifies the type
   * of encoding which will be used, whereas after CBOR decoding this indicates
   * the type of encoding encountered in the original representation.
   *
   * @return Returns a boolean value which will be set to 'true' if the
   *   encapsulated data uses indefinite length encoding.
   */
  public boolean isIndefiniteLength();

  /**
   * Accesses a copy of the tags which are associated with a CBOR data item. These
   * are presented in the order in which they are included in the CBOR
   * representation. This means that when multiple tags are present, the one with
   * the highest index value binds most closely to the encapsulated data.
   *
   * @return Returns an array of integer tag values which are associated with a
   *   CBOR data item.
   */
  public int[] getTags();

  /**
   * Assigns a new decoding status value to the data item. If the new decoding
   * status is a failure condition it effectively invalidates the remaining data
   * item fields.
   *
   * @param decodeStatus This is the new decoding status which is to be associated
   *   with the data item.
   * @return Returns a reference to the data item instance, allowing fluent
   *   assignment of the decode status value.
   * @throws IllegalStateException This exception will be thrown on attempting to
   *   assign a successful decode status value if the decode status has previously
   *   been set to a failure condition, resulting in the invalidation of the
   *   remaining data item fields.
   */
  public DataItem<T> setDecodeStatus(DecodeStatus decodeStatus) throws IllegalStateException;

  /**
   * Determines the decoding status which is associated with this data item. The
   * remaining data item fields should only be considered valid if the returned
   * decoding status is not a failure condition.
   *
   * @return Returns the status of the decoding operation which was carried out on
   *   the data item.
   */
  public DecodeStatus getDecodeStatus();

}
