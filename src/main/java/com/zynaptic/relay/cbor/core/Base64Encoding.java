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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Provides various utility functions for base 64 encoding and decoding.
 *
 * @author Chris Holgate
 */
final class Base64Encoding {

  // Statically defined lookup table for base 64 decoding.
  private static final byte[] BASE64_DECODING_TABLE;

  // Statically defined lookup table for standard base 64 decoding.
  private static final char[] BASE64_ENCODING_TABLE;

  // Statically defined lookup table for URL-safe base 64 encoding.
  private static final char[] BASE64_URL_ENCODING_TABLE;

  /**
   * Performs default encoding for transporting binary arrays in JSON strings.
   * This uses the Base64-URL format without padding, as specified in RFC4648
   * section 5.
   *
   * @param printWriter This is the output print writer to which the encoded
   *   string will be appended.
   * @param byteArray This is the byte array which is to be encoded using
   *   Base64-URL.
   */
  static void writeBase64Url(final PrintWriter printWriter, final byte[] byteArray) {
    encodeBase64(printWriter, byteArray, 0, byteArray.length, BASE64_URL_ENCODING_TABLE, false);
  }

  /**
   * Performs default encoding for transporting segmented binary arrays in JSON
   * strings. This uses the Base64-URL format without padding, as specified in
   * RFC4648 section 5.
   *
   * @param printWriter This is the output print writer to which the encoded
   *   string will be appended.
   * @param byteArray This is the sequence of byte array segments which are to be
   *   concatenated and encoded using Base64-URL.
   */
  static void writeBase64Url(final PrintWriter printWriter, final List<byte[]> byteArrayList) {
    encodeBase64List(printWriter, byteArrayList, BASE64_URL_ENCODING_TABLE, false);
  }

  /**
   * Decode a string of base 64 encoded characters. This uses a common lookup
   * table for both of the standard encoding alphabets (Base64 and Base64-URL).
   * The results are appended to the supplied byte oriented output stream.
   *
   * @param outputStream This is the byte oriented output stream to which the
   *   decoded characters are to be written.
   * @param encodedString This is a string which should contain valid base 64
   *   encoded data.
   * @param checkPadding This is a boolean value which when set to 'true'
   *   indicates that the encoded string will be checked for valid padding
   *   characters.
   * @return Returns a boolean value which will be set to 'true' only if decoding
   *   was successful for the entire contents of the encoded string.
   * @throws IOException This exception will be thrown if an error was encountered
   *   while attempting to write the decoded data to the specified output stream.
   */
  static boolean decodeBase64(final OutputStream outputStream, final String encodedString, final boolean checkPadding)
      throws IOException {
    int i, j, length;
    int shiftReg = 0;
    byte tableEntry;

    // Perform padding checks if required.
    length = encodedString.length();
    if (checkPadding) {
      if ((length & 0x03) != 0) {
        return false;
      }
      if (encodedString.charAt(length - 1) == '=') {
        length -= 1;
        if (encodedString.charAt(length - 1) == '=') {
          length -= 1;
        }
      }
    }

    // Process all groups of four characters.
    for (i = 0; i < length - 3; i += 4) {
      for (j = 0; j < 4; j++) {
        final char nextChar = encodedString.charAt(i + j);
        if ((nextChar < 0) || (nextChar > 127) || ((tableEntry = BASE64_DECODING_TABLE[nextChar]) == -1)) {
          return false;
        }
        shiftReg = (shiftReg << 6) | tableEntry;
      }
      for (j = 0; j < 3; j++) {
        outputStream.write(shiftReg >> 16);
        shiftReg <<= 8;
      }
    }

    // Processed all the string as groups of 4 characters.
    if ((length - i) == 0) {
      return true;
    }

    // Process final group of three characters (maps to two bytes).
    else if ((length - i) == 3) {
      for (j = 0; j < 3; j++) {
        final char nextChar = encodedString.charAt(i + j);
        if ((nextChar < 0) || (nextChar > 127) || ((tableEntry = BASE64_DECODING_TABLE[nextChar]) == -1)) {
          return false;
        }
        shiftReg = (shiftReg << 6) | tableEntry;
      }
      shiftReg <<= 6;
      outputStream.write(shiftReg >> 16);
      outputStream.write(shiftReg >> 8);
      return true;
    }

    // Process final group of two characters (maps to one byte).
    else if ((length - i) == 2) {
      for (j = 0; j < 2; j++) {
        final char nextChar = encodedString.charAt(i + j);
        if ((nextChar < 0) || (nextChar > 127) || ((tableEntry = BASE64_DECODING_TABLE[nextChar]) == -1)) {
          return false;
        }
        shiftReg = (shiftReg << 6) | tableEntry;
      }
      shiftReg <<= 12;
      outputStream.write(shiftReg >> 16);
      return true;
    }

    // Invalid number of encoded characters detected in string.
    else {
      return false;
    }
  }

  /*
   * Performs base64 encoding on a list of byte array segments as if they were a
   * single contiguous byte sequence.
   */
  private static void encodeBase64List(final PrintWriter printWriter, final List<byte[]> byteArrayList,
      final char[] lookupTable, final boolean addPadding) {
    int offset, length;
    int shimSize = 0;
    final byte[] shimBytes = new byte[3];

    // Iterate over all of the byte array segments, processing each in multiples
    // of 3 bytes.
    for (final byte[] byteArray : byteArrayList) {
      if (shimSize == 0) {
        offset = 0;
        length = 3 * (byteArray.length / 3);
      }

      // Write shim data when there is 1 residual byte from the previous
      // segment.
      else if (shimSize == 1) {
        offset = 2;
        length = 3 * ((byteArray.length - 2) / 3);
        shimBytes[1] = byteArray[0];
        shimBytes[2] = byteArray[1];
        encodeBase64(printWriter, shimBytes, 0, 3, lookupTable, false);
      }

      // Write shim data when there are 2 residual bytes from the previous
      // segment.
      else {
        offset = 1;
        length = 3 * ((byteArray.length - 1) / 3);
        shimBytes[2] = byteArray[0];
        encodeBase64(printWriter, shimBytes, 0, 3, lookupTable, false);
      }

      // Write the contents of the byte array segment then add the extra shim
      // bytes if required.
      encodeBase64(printWriter, byteArray, offset, length, lookupTable, false);
      if (offset + length == byteArray.length) {
        shimSize = 0;
      } else if (offset + length == byteArray.length - 1) {
        shimBytes[0] = byteArray[offset + length];
        shimSize = 1;
      } else {
        shimBytes[0] = byteArray[offset + length];
        shimBytes[1] = byteArray[offset + length + 1];
        shimSize = 2;
      }
    }

    // Write out the final shim bytes if required.
    if (shimSize != 0) {
      encodeBase64(printWriter, shimBytes, 0, shimSize, lookupTable, false);
    }
  }

  /*
   * Encode multiple source bytes using base 64 encoding with a specific lookup
   * table. Length must be a multiple of 3 unless this is the final block of data
   * being encoded into a JSON string, in which case the final two or three coding
   * characters are generated with padding inserted if required.
   */
  private static void encodeBase64(final PrintWriter printWriter, final byte[] buffer, int offset, final int length,
      final char[] lookupTable, final boolean addPadding) {
    int i, j;
    int shiftReg = 0;

    // Process all groups of three bytes.
    for (i = 0; i < length - 2; i += 3) {
      for (j = 0; j < 3; j++) {
        shiftReg = (shiftReg << 8) | (0xFF & buffer[offset++]);
      }
      for (j = 0; j < 4; j++) {
        printWriter.append(lookupTable[0x3F & (shiftReg >> 18)]);
        shiftReg <<= 6;
      }
    }

    // Process final group of two bytes.
    if ((length - i) == 2) {
      shiftReg = ((0xFF & buffer[offset++]) << 16);
      shiftReg |= ((0xFF & buffer[offset++]) << 8);
      for (j = 0; j < 3; j++) {
        printWriter.append(lookupTable[0x3F & (shiftReg >> 18)]);
        shiftReg = (shiftReg << 6);
      }
      if (addPadding) {
        printWriter.append('=');
      }
    }

    // Process final group of one byte.
    else if ((length - i) == 1) {
      shiftReg = ((0xFF & buffer[offset++]) << 16);
      for (j = 0; j < 2; j++) {
        printWriter.append(lookupTable[0x3F & (shiftReg >> 18)]);
        shiftReg = (shiftReg << 6);
      }
      if (addPadding) {
        printWriter.append('=');
        printWriter.append('=');
      }
    }
  }

  /*
   * Build the encoding and decoding tables.
   */
  static {
    int i;
    BASE64_DECODING_TABLE = new byte[128];
    BASE64_ENCODING_TABLE = new char[64];
    BASE64_URL_ENCODING_TABLE = new char[64];

    // Build the common decoding table. This contains character mappings for
    // both base64 and base64url lookups.
    for (i = 0; i < '0'; i++) {
      BASE64_DECODING_TABLE[i] = -1;
    }
    for (i = '0'; i <= '9'; i++) {
      BASE64_DECODING_TABLE[i] = (byte) (i - '0' + 52);
    }
    for (i = '9' + 1; i < 'A'; i++) {
      BASE64_DECODING_TABLE[i] = -1;
    }
    for (i = 'A'; i <= 'Z'; i++) {
      BASE64_DECODING_TABLE[i] = (byte) (i - 'A');
    }
    for (i = 'Z' + 1; i < 'a'; i++) {
      BASE64_DECODING_TABLE[i] = -1;
    }
    for (i = 'a'; i <= 'z'; i++) {
      BASE64_DECODING_TABLE[i] = (byte) (i - 'a' + 26);
    }
    for (i = 'z' + 1; i < 128; i++) {
      BASE64_DECODING_TABLE[i] = -1;
    }
    BASE64_DECODING_TABLE['+'] = 62;
    BASE64_DECODING_TABLE['/'] = 63;
    BASE64_DECODING_TABLE['-'] = 62;
    BASE64_DECODING_TABLE['_'] = 63;

    // Build the standard base64 encoding table.
    for (i = 0; i < 62; i++) {
      if (i < 26) {
        BASE64_ENCODING_TABLE[i] = (char) (i + 'A');
      } else if (i < 52) {
        BASE64_ENCODING_TABLE[i] = (char) ((i - 26) + 'a');
      } else {
        BASE64_ENCODING_TABLE[i] = (char) ((i - 52) + '0');
      }
    }
    BASE64_ENCODING_TABLE[62] = '+';
    BASE64_ENCODING_TABLE[63] = '/';

    // Build the base64url encoding table.
    for (i = 0; i < 62; i++) {
      if (i < 26) {
        BASE64_URL_ENCODING_TABLE[i] = (char) (i + 'A');
      } else if (i < 52) {
        BASE64_URL_ENCODING_TABLE[i] = (char) ((i - 26) + 'a');
      } else {
        BASE64_URL_ENCODING_TABLE[i] = (char) ((i - 52) + '0');
      }
    }
    BASE64_URL_ENCODING_TABLE[62] = '-';
    BASE64_URL_ENCODING_TABLE[63] = '_';
  }
}
