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
 * Specifies the enumerated set of CBOR extension data types and their
 * associated integer encoding. This includes definitions for floating point
 * numbers and simple values with no content. See RFC7049 section 2.3 for CBOR
 * extension type definitions.
 *
 * @author Chris Holgate
 */
public enum ExtensionType {

  /**
   * This CBOR extension data type specifies a boolean data item which is assigned
   * the implicit value of 'false'.
   */
  FALSE(20),

  /**
   * This CBOR extension data type specifies a boolean data item which is assigned
   * the implicit value of 'true'.
   */
  TRUE(21),

  /**
   * This CBOR extension data type specifies that a CBOR data item has been
   * assigned a null reference.
   */
  NULL(22),

  /**
   * This CBOR extension data type specifies that a CBOR data item has been
   * assigned an undefined value.
   */
  UNDEFINED(23),

  /**
   * This CBOR extension data type specifies a simple value which has undefined
   * semantics. Values 0 to 19 are encoded in the initial data type byte, with
   * values 32 to 255 encoded in a single subsequent value byte.
   */
  SIMPLE(24),

  /**
   * This CBOR extension data type is used to indicate that the following two data
   * bytes are to be interpreted as a 16-bit IEEE-754 half precision floating
   * point value.
   */
  FLOAT16(25),

  /**
   * This CBOR extension data type is used to indicate that the following four
   * data bytes are to be interpreted as a 32-bit IEEE-754 single precision
   * floating point value.
   */
  FLOAT32(26),

  /**
   * This CBOR extension data type is used to indicate that the following eight
   * data bytes are to be interpreted as a 64-bit IEEE-754 double precision
   * floating point value.
   */
  FLOAT64(27),

  /**
   * This CBOR extension data type value is used to provide the "break" stop code
   * for indefinite length data items.
   */
  BREAK(31);

  /**
   * Gets the enumerated extension data type which is associated with the
   * specified byte encoding, as extracted from the initial data item byte.
   *
   * @param byteEncoding This is the byte encoding which is to be mapped to the
   *   corresponding extension data type. A null reference always maps to a null
   *   extension type.
   * @return Returns the extension data type which corresponds to the specified
   *   byte encoding, or a null reference if there is no corresponding extension
   *   data type.
   */
  public static final ExtensionType getExtensionType(final Byte byteEncoding) {
    if (byteEncoding == null) {
      return null;
    }

    // Extracts explicitly defined extension types and then maps the remaining
    // unassigned values from RFC7049 to the simple value data type.
    else {
      final byte maskedByteEncoding = (byte) (byteEncoding.intValue() & 0x1F);
      ExtensionType resolvedType = encodingMap.get(maskedByteEncoding);
      if (resolvedType == null) {
        if ((maskedByteEncoding >= 0) && (maskedByteEncoding <= 19)) {
          resolvedType = SIMPLE;
        }
      }
      return resolvedType;
    }
  }

  /**
   * Gets the numeric extension type identifier used by the CBOR encoding. Always
   * returns the encoding value of 24 for simple data types.
   *
   * @return Returns a byte value which is used to identify the CBOR extension
   *   type.
   */
  public byte getByteEncoding() {
    return byteEncoding;
  }

  // Specify the mapping of integer encoding values to content types.
  private static final Map<Byte, ExtensionType> encodingMap;

  // Specify the encoding parameter which is associated with each option code.
  private final byte byteEncoding;

  /*
   * Default constructor initialises the extension data type encoding from the
   * supplied extension type identifier.
   */
  private ExtensionType(final int byteEncoding) {
    this.byteEncoding = (byte) byteEncoding;
  }

  /*
   * Static constructor builds the encoding map on startup.
   */
  static {
    encodingMap = new TreeMap<Byte, ExtensionType>();
    for (final ExtensionType extensionType : EnumSet.allOf(ExtensionType.class)) {
      final Byte key = extensionType.getByteEncoding();
      if (encodingMap.containsKey(key)) {
        throw new RuntimeException("Duplicate extension type encodings detected.");
      }
      encodingMap.put(key, extensionType);
    }
  };
}
