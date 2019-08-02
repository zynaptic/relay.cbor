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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides JSON data item decoding capabilities. The data item decoder may be
 * used to parse a single data item per invocation, returning the decoding state
 * and providing subsequent access to the decoded data item.
 *
 * @author Chris Holgate
 */
final class JsonItemDecoder {

  // Specifies the data input stream to be used by the decoder.
  private final InputStreamReader inputStream;

  // Provides single character local buffer.
  private int currentChar;

  /**
   * Default constructor assigns the decoder to a given input stream reader.
   *
   * @param inputStream This is the input stream reader with which the decoder is
   *   associated.
   */
  JsonItemDecoder(final InputStreamReader inputStream) {
    this.inputStream = inputStream;
  }

  /**
   * Performs a single data item decoding step. This processes the next JSON data
   * item in the input stream, returning the decoding status on completion.
   *
   * @return Returns the decoded data item which also encapsulates the decoding
   *   status that reflects the success or failure of the decoding operation.
   * @throws IOException This exception will be thrown on failure to read a
   *   complete data item from the input data stream.
   */
  DataItem<?> decode() throws IOException {
    discardNextCharWhitespace();
    return recursiveDecode();
  }

  /*
   * Implements recursive decoding. On entry the current character buffer must
   * contain the first character in the next JSON token.
   */
  private DataItem<?> recursiveDecode() throws IOException {
    DataItem<?> dataItem;
    switch (currentChar) {

    // Character read operations which fail with EOF return a value of -1, which
    // is used here to infer an invalid data item.
    case -1:
      dataItem = UndefinedDataItem.INVALID;
      break;

    // Identify the start of a JSON array for subsequent parsing.
    case '[':
      dataItem = processJsonArray();
      break;

    // Identify the start of a JSON object for subsequent parsing.
    case '{':
      dataItem = processJsonObject();
      break;

    // Identify the start of a JSON string for subsequent parsing.
    case '\"':
      dataItem = processJsonString();
      break;

    // Identify the start of a JSON null value for subsequent parsing.
    case 'n':
      dataItem = processJsonNull();
      break;

    // Identify the start of a JSON true value for subsequent parsing.
    case 't':
      dataItem = processJsonTrue();
      break;

    // Identify the start of a JSON false value for subsequent parsing.
    case 'f':
      dataItem = processJsonFalse();
      break;

    // Treat all remaining cases as JSON numeric values, regardless of whether
    // the current character is valid.
    default:
      dataItem = processJsonNumeric();
      break;
    }
    return dataItem;
  }

  /*
   * Searches for first non-whitespace character, starting with the current
   * character. Then overwrites the current character with the result.
   */
  private void discardCurrentCharWhitespace() throws IOException {
    if (currentChar == '/') {
      discardComment();
    }
    if (Character.isWhitespace(currentChar)) {
      discardNextCharWhitespace();
    }
  }

  /*
   * Searches for first non-whitespace character, starting with the next character
   * in the input stream. Then overwrites the current character with the result.
   */
  private void discardNextCharWhitespace() throws IOException {
    do {
      currentChar = inputStream.read();
      if (currentChar == '/') {
        discardComment();
      }
    } while (Character.isWhitespace(currentChar));
  }

  /*
   * Performs common comment discarding, forcing an EOF condition if the comment
   * is not valid.
   */
  private void discardComment() throws IOException {
    currentChar = inputStream.read();
    switch (currentChar) {
    case '/':
      discardSingleLineComment();
      break;
    case '*':
      discardMultiLineComment();
      break;
    default:
      currentChar = -1;
      break;
    }
  }

  /*
   * Perform single line comment discarding. This discards all characters up to
   * the end of the line.
   */
  private void discardSingleLineComment() throws IOException {
    do {
      currentChar = inputStream.read();
    } while ((currentChar >= 0) && (currentChar != '\n'));
  }

  /*
   * Perform multiple line comment discarding. This discards all characters up to
   * the comment close token.
   */
  private void discardMultiLineComment() throws IOException {
    int lastChar;
    currentChar = inputStream.read();
    do {
      if (currentChar < 0) {
        return;
      }
      lastChar = currentChar;
      currentChar = inputStream.read();
    } while (!((lastChar == '*') && (currentChar == '/')));
    currentChar = inputStream.read();
  }

  /*
   * Process the JSON array contents.
   */
  private DataItem<?> processJsonArray() throws IOException {
    DecodeStatus decodeStatus = DecodeStatus.TRANSLATABLE;

    // Align current character to start of first data item, detecting empty
    // arrays in the process.
    discardNextCharWhitespace();
    if (currentChar == ']') {
      currentChar = inputStream.read();
      return new ArrayDataItem(new ArrayList<DataItem<?>>(0), null, true).setDecodeStatus(decodeStatus);
    }

    // Recursively process array entries until the end of the JSON array element
    // is detected.
    final LinkedList<DataItem<?>> jsonArrayList = new LinkedList<DataItem<?>>();
    while (true) {

      // Extract the next list data item. All data types are valid.
      final DataItem<?> nextDataItem = recursiveDecode();
      decodeStatus = decodeStatus.getLeastStrictCriteria(nextDataItem.getDecodeStatus());
      if (decodeStatus.isFailure()) {
        return new UndefinedDataItem(decodeStatus);
      }

      // Append each data item to the array entry list and check for comma
      // separator or closing brace.
      jsonArrayList.add(nextDataItem);
      discardCurrentCharWhitespace();
      if (currentChar == ']') {
        currentChar = inputStream.read();
        return new ArrayDataItem(jsonArrayList, null, false).setDecodeStatus(decodeStatus);
      } else if (currentChar != ',') {
        return UndefinedDataItem.INVALID;
      }
      discardNextCharWhitespace();
    }
  }

  /*
   * Process the JSON object contents.
   */
  private DataItem<?> processJsonObject() throws IOException {
    DecodeStatus decodeStatus = DecodeStatus.TRANSLATABLE;

    // Align current character to start of first data item, detecting empty
    // object maps in the process.
    discardNextCharWhitespace();
    if (currentChar == '}') {
      currentChar = inputStream.read();
      return new NamedMapDataItem(null, null, true).setDecodeStatus(decodeStatus);
    }

    // Recursively process array entries until the end of the JSON array element
    // is detected.
    final Map<String, DataItem<?>> jsonObjectMap = new HashMap<String, DataItem<?>>();
    while (true) {

      // Extract the key data item - only text strings are supported in JSON.
      final DataItem<?> keyDataItem = recursiveDecode();
      decodeStatus = decodeStatus.getLeastStrictCriteria(keyDataItem.getDecodeStatus());
      if (decodeStatus.isFailure()) {
        return new UndefinedDataItem(decodeStatus);
      }
      if (keyDataItem.getDataType() != UserDataType.TEXT_STRING) {
        return UndefinedDataItem.INVALID;
      }

      // Check for the map element separator.
      discardCurrentCharWhitespace();
      if (currentChar != ':') {
        return UndefinedDataItem.INVALID;
      }

      // Extract the value data item.
      discardNextCharWhitespace();
      final DataItem<?> valueDataItem = recursiveDecode();
      decodeStatus = decodeStatus.getLeastStrictCriteria(keyDataItem.getDecodeStatus());
      if (decodeStatus.isFailure()) {
        return new UndefinedDataItem(decodeStatus);
      }

      // Check for duplicate keys. These may be well formed, but are not
      // translatable as only the first duplicate key entry is retained.
      final String key = keyDataItem.castData();
      if (jsonObjectMap.containsKey(key)) {
        decodeStatus.getLeastStrictCriteria(DecodeStatus.WELL_FORMED);
      } else {
        jsonObjectMap.put(key, valueDataItem);
      }

      // Check for comma separator or closing brace.
      discardCurrentCharWhitespace();
      if (currentChar == '}') {
        currentChar = inputStream.read();
        return new NamedMapDataItem(jsonObjectMap, null, false).setDecodeStatus(decodeStatus);
      } else if (currentChar != ',') {
        return UndefinedDataItem.INVALID;
      }
      discardNextCharWhitespace();
    }
  }

  /*
   * Process the JSON string contents. On reading the characters from the JSON
   * string, the escaped characters are replaced with their actual Unicode values.
   */
  private DataItem<?> processJsonString() throws IOException {
    final StringBuilder stringBuilder = new StringBuilder();
    int nextChar;

    // Copies over all characters except for closing quotes and escaped values.
    // Also traps end of file conditions.
    while ((nextChar = inputStream.read()) != '\"') {
      if (nextChar < 0) {
        return UndefinedDataItem.INVALID;
      } else if (nextChar != '\\') {
        stringBuilder.appendCodePoint(nextChar);
      }

      // Perform escaped character mapping.
      else {
        nextChar = inputStream.read();
        switch (nextChar) {

        // Perform direct mapping of standard escaped characters.
        case '"':
        case '\\':
        case '/':
          stringBuilder.appendCodePoint(nextChar);
          break;

        // Map escaped control code characters.
        case 'b':
          stringBuilder.append('\b');
          break;
        case 'f':
          stringBuilder.append('\f');
          break;
        case 'n':
          stringBuilder.append('\n');
          break;
        case 'r':
          stringBuilder.append('\r');
          break;
        case 't':
          stringBuilder.append('\t');
          break;

        // Extract the UTF codepoint value from four consecutive hex digits.
        case 'u':
          nextChar = 0;
          for (int i = 0; i < 4; i++) {
            final int hexVal = Character.digit(inputStream.read(), 16);
            if (hexVal < 0) {
              return UndefinedDataItem.INVALID;
            } else {
              nextChar = (nextChar << 4) + hexVal;
            }
          }
          stringBuilder.append((char) nextChar);
          break;
        default:
          return UndefinedDataItem.INVALID;
        }
      }
    }
    currentChar = inputStream.read();
    return new TextStringDataItem(stringBuilder.toString(), null).setDecodeStatus(DecodeStatus.TRANSLATABLE);
  }

  /*
   * Process the JSON null value.
   */
  private DataItem<?> processJsonNull() throws IOException {
    if ((inputStream.read() != 'u') || (inputStream.read() != 'l') || (inputStream.read() != 'l')) {
      return UndefinedDataItem.INVALID;
    }
    currentChar = inputStream.read();
    return new UndefinedDataItem(true, null).setDecodeStatus(DecodeStatus.TRANSLATABLE);
  }

  /*
   * Process the JSON true value.
   */
  private DataItem<?> processJsonTrue() throws IOException {
    if ((inputStream.read() != 'r') || (inputStream.read() != 'u') || (inputStream.read() != 'e')) {
      return UndefinedDataItem.INVALID;
    }
    currentChar = inputStream.read();
    return new BooleanDataItem(true, null).setDecodeStatus(DecodeStatus.TRANSLATABLE);
  }

  /*
   * Process the JSON false value.
   */
  private DataItem<?> processJsonFalse() throws IOException {
    if ((inputStream.read() != 'a') || (inputStream.read() != 'l') || (inputStream.read() != 's')
        || (inputStream.read() != 'e')) {
      return UndefinedDataItem.INVALID;
    }
    currentChar = inputStream.read();
    return new BooleanDataItem(false, null).setDecodeStatus(DecodeStatus.TRANSLATABLE);
  }

  /*
   * Process JSON numeric values.
   */
  private DataItem<?> processJsonNumeric() throws IOException {
    boolean negate = false;
    boolean encodeAsInteger = true;
    long mantissa = 0;
    int exponent = 0;
    int fractionLength = 0;
    int charVal;

    // Check for negative values and ensure that a valid leading digit is
    // present.
    if (currentChar == '-') {
      negate = true;
      currentChar = inputStream.read();
    }
    if (Character.isDigit(currentChar) == false) {
      return UndefinedDataItem.INVALID;
    }

    // Process leading digits up to the decimal point or exponent marker.
    while ((charVal = Character.digit(currentChar, 10)) >= 0) {
      mantissa = mantissa * 10 + charVal;
      currentChar = inputStream.read();
    }

    // Add fractional part if present.
    if (currentChar == '.') {
      encodeAsInteger = false;
      currentChar = inputStream.read();
      while ((charVal = Character.digit(currentChar, 10)) >= 0) {
        mantissa = mantissa * 10 + charVal;
        fractionLength = fractionLength + 1;
        currentChar = inputStream.read();
      }
    }

    // Negate the mantissa if required, prior to processing the exponent.
    if (negate == true) {
      negate = false;
      mantissa = -mantissa;
    }

    // Process exponential term if present.
    if ((currentChar == 'e') || (currentChar == 'E')) {
      encodeAsInteger = false;
      currentChar = inputStream.read();

      // Check for negative exponents and discard the redundant positive
      // exponent indicator.
      if (currentChar == '-') {
        negate = true;
        currentChar = inputStream.read();
      } else if (currentChar == '+') {
        currentChar = inputStream.read();
      }

      // Extract the exponent digits and then correct for the number of
      // fractional digits.
      while ((charVal = Character.digit(currentChar, 10)) >= 0) {
        exponent = exponent * 10 + charVal;
        currentChar = inputStream.read();
      }
      if (negate == true) {
        exponent = -exponent;
      }
    }

    // Encode as an integer if no decimal point or exponent term was used.
    // Otherwise use double precision floating point.
    if (encodeAsInteger == true) {
      return new IntegerDataItem(mantissa, null).setDecodeStatus(DecodeStatus.TRANSLATABLE);
    } else {
      double floatValue = mantissa;
      exponent = exponent - fractionLength;
      if (exponent != 0) {
        floatValue *= Math.pow(10, exponent);
      }
      return new FloatDoubleDataItem(floatValue, null).setDecodeStatus(DecodeStatus.TRANSLATABLE);
    }
  }
}
