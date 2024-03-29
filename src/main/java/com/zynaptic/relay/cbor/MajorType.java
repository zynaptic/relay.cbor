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

import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Specifies the enumerated set of CBOR major data types and their associated
 * integer encoding. See RFC7049 section 2.1 for CBOR major type definitions.
 *
 * @author Chris Holgate
 */
public enum MajorType {

  /**
   * This CBOR major data type is used to encode unsigned integer values in the
   * range from 0 to 2^64-1.
   */
  UNSIGNED(0),

  /**
   * This CBOR major data type is used to encode negative integer values in the
   * range from -1 down to -2^64.
   */
  NEGATIVE(1),

  /**
   * This CBOR major data type is used to identify arbitrary strings of 8-bit
   * bytes.
   */
  BYTE_STRING(2),

  /**
   * This CBOR major data type is used to identify UTF-8 encoded unicode text
   * strings.
   */
  TEXT_STRING(3),

  /**
   * This CBOR major data type is used to identify ordered arrays of other data
   * items.
   */
  ARRAY(4),

  /**
   * This CBOR major data type is used to identify mappings between pairs of data
   * items.
   */
  MAP(5),

  /**
   * This CBOR major data type is used to identify semantic tags which provide
   * additional information about a nested data item.
   */
  TAG(6),

  /**
   * This CBOR major data type is used to identify extension data types which
   * include simple data, floating point and boolean values.
   */
  EXTENSION(7);

  /**
   * Gets the enumerated major data type which is associated with the specified
   * byte encoding. The associated additional information field is automatically
   * discarded during this lookup process.
   *
   * @param byteEncoding This is the byte encoding which is to be mapped to the
   *   corresponding major data type. The lower 5 bits which represent the
   *   additional information field are automatically discarded. A null reference
   *   always maps to a null message type.
   * @return Returns the major data type which corresponds to the specified byte
   *   encoding, or a null reference if there is no corresponding major data type.
   */
  public static final MajorType getMajorType(final Byte byteEncoding) {
    if (byteEncoding == null) {
      return null;
    } else {
      return encodingMap.get((byte) (0xE0 & byteEncoding));
    }
  }

  /**
   * Gets the major data type byte which is used to represent the major data type
   * and associated additional information field. These conform to the bit
   * positions described in RFC7049 section 2.
   *
   * @param additionalInfo This is the additional information field which is to be
   *   included in the generated byte encoding.
   * @return Returns the major data type byte encoding which may be used as the
   *   first byte of a CBOR data item.
   */
  public byte getByteEncoding(final int additionalInfo) {
    return (byte) (byteEncoding | (0x1F & additionalInfo));
  }

  // Specify the mapping of byte encoding values to response codes.
  private static final Map<Byte, MajorType> encodingMap;

  // Encoded major type identifier, including the additional information field.
  private byte byteEncoding;

  /*
   * Default constructor initialises the major data type byte from the supplied
   * major type identifier. This encoding also includes the additional information
   * field, which is set to zero.
   */
  private MajorType(final int majorTypeId) {
    byteEncoding = (byte) (majorTypeId * 0x20);
  }

  /*
   * Static constructor builds the encoding map on startup.
   */
  static {
    encodingMap = new TreeMap<Byte, MajorType>();
    for (final MajorType majorType : EnumSet.allOf(MajorType.class)) {
      final Byte key = majorType.getByteEncoding(0);
      if (encodingMap.containsKey(key)) {
        throw new RuntimeException("Duplicate major type encodings detected.");
      }
      encodingMap.put(key, majorType);
    }
  };
}
