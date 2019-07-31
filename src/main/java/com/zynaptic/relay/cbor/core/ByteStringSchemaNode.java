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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DataItemFactory;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.InvalidSchemaException;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Extends generic CBOR data schema nodes for use with the encoded byte string
 * data types. Base64-URL is the default encoding scheme for binary data, but
 * both standard and URL-safe decoding is supported.
 *
 * @author Chris Holgate
 */
final class ByteStringSchemaNode extends SchemaNodeCore {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  // Specifies the default value associated with the schema node.
  private final byte[] defaultValue;

  // Specifies the maximum number of bytes in the underlying UTF-8 string.
  private final int maxLength;

  // Specifies the minimum number of bytes in the underlying UTF-8 string.
  private final int minLength;

  /**
   * Provides private default constructor which associates the schema node with a
   * given CBOR data item factory.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param defaultValue This is the default value associated with the byte string
   *   node.
   * @param maxLength This specifies the maximum number of bytes which may be
   *   represented in the byte string (inclusive).
   * @param minLength This specifies the minimum number of bytes which may be
   *   represented in the byte string (inclusive).
   */
  private ByteStringSchemaNode(final DataItemFactory dataItemFactory, final byte[] defaultValue, final int maxLength,
      final int minLength) {
    this.dataItemFactory = dataItemFactory;
    this.defaultValue = defaultValue;
    this.maxLength = maxLength;
    this.minLength = minLength;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new byte string schema node.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param schemaMap This is the attribute map which is to be processed in order
   *   to generate the schema node.
   * @param schemaPath This is the hierarchical path to the schema node to be used
   *   for error reporting.
   */
  static ByteStringSchemaNode build(final DataItemFactory dataItemFactory, final Map<String, DataItem<?>> schemaMap,
      final String schemaPath) throws InvalidSchemaException {

    // Extract the default value, if present.
    byte[] defaultValue = new byte[0];
    DataItem<?> defaultItem = schemaMap.get("default");
    if (defaultItem != null) {
      defaultItem = performAutoDecoding(dataItemFactory, defaultItem, defaultItem.getDecodeStatus(), null, null);
      if ((defaultItem.getDataType() != UserDataType.BYTE_STRING)
          && (defaultItem.getDataType() != UserDataType.BYTE_STRING_LIST)) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'default' field not valid.");
      }
      defaultValue = getByteString(defaultItem);
    }
    final int defaultLength = defaultValue.length;

    // Extract the maximum string length which is associated with the schema
    // node.
    long maxLength = Integer.MAX_VALUE;
    final DataItem<?> maxLengthItem = schemaMap.get("maxLength");
    if (maxLengthItem != null) {
      if (maxLengthItem.getDataType() != UserDataType.INTEGER) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'maxLength' field not valid.");
      }
      maxLength = (Long) maxLengthItem.getData();
    }

    // Extract the minimum string length which is associated with the schema
    // node.
    long minLength = 0;
    final DataItem<?> minLengthItem = schemaMap.get("minLength");
    if (minLengthItem != null) {
      if (minLengthItem.getDataType() != UserDataType.INTEGER) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'minLength' field not valid.");
      }
      minLength = (Long) minLengthItem.getData();
    }

    // Perform sanity checking on the maximum and minimum string lengths.
    if ((minLength < 0) || (maxLength < minLength) || (maxLength > Integer.MAX_VALUE)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR schema 'minLength' and 'maxLength' fields not valid.");
    }
    if ((defaultLength < minLength) || (defaultLength > maxLength)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR schema 'default' field not valid length.");
    }
    return new ByteStringSchemaNode(dataItemFactory, defaultValue, (int) maxLength, (int) minLength);
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.BYTE_STRING;
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  ByteStringSchemaNode duplicate() {
    final ByteStringSchemaNode duplicatedSchema = new ByteStringSchemaNode(dataItemFactory, defaultValue, maxLength,
        minLength);
    duplicatedSchema.copyParamsFrom(this);
    return duplicatedSchema;
  }

  /*
   * Implements SchemaNodeCore.createDefault(...)
   */
  @Override
  DataItem<byte[]> createDefault(final boolean includeAll) {
    return dataItemFactory.createByteStringItem((defaultValue == null) ? null : defaultValue.clone(), getTagValues());
  }

  /*
   * Implements SchemaNodeCore.validate(...)
   */
  @Override
  boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized, final boolean doRecursive,
      final Logger logger, final String loggerPath) {
    final DataItem<?> decodedDataItem = performAutoDecoding(dataItemFactory, sourceDataItem,
        sourceDataItem.getDecodeStatus(), logger, loggerPath);
    return doValidation(decodedDataItem, logger, loggerPath);
  }

  /*
   * Implements SchemaNodeCore.expand(...)
   */
  @Override
  DataItem<?> expand(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {
    final DataItem<?> decodedDataItem = performAutoDecoding(dataItemFactory, sourceDataItem, DecodeStatus.EXPANDED,
        logger, loggerPath);
    if (!doValidation(decodedDataItem, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    } else {
      return decodedDataItem;
    }
  }

  /*
   * Implements SchemaNodeCore.tokenize(...)
   */
  @Override
  DataItem<?> tokenize(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {
    final DataItem<?> decodedDataItem = performAutoDecoding(dataItemFactory, sourceDataItem, DecodeStatus.TOKENIZED,
        logger, loggerPath);
    if (!doValidation(decodedDataItem, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    } else {
      return decodedDataItem;
    }
  }

  /*
   * Perform validation on automatically decoded data.
   */
  private boolean doValidation(final DataItem<?> decodedDataItem, final Logger logger, final String loggerPath) {
    if ((decodedDataItem.getDataType() != UserDataType.BYTE_STRING)
        && (decodedDataItem.getDataType() != UserDataType.BYTE_STRING_LIST)) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because source data is not a valid byte string.");
      }
      return false;
    }

    // Check the specified byte string value against the valid length range.
    final int byteArrayLength = getByteStringLength(decodedDataItem);
    if ((byteArrayLength < minLength) || (byteArrayLength > maxLength)) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because source byte array length " + byteArrayLength
            + " is outside range " + minLength + " to " + maxLength);
      }
      return false;
    }
    return true;
  }

  /*
   * Calculate the length of a standard byte array data item or concatenated list.
   */
  private int getByteStringLength(final DataItem<?> sourceDataItem) {
    int byteStringLength;

    // Extract a single byte string length.
    if (sourceDataItem.getDataType() == UserDataType.BYTE_STRING) {
      byteStringLength = ((byte[]) sourceDataItem.getData()).length;
    }

    // Add lengths from byte string list.
    else if (sourceDataItem.getDataType() == UserDataType.BYTE_STRING_LIST) {
      byteStringLength = 0;
      final List<byte[]> byteStringList = sourceDataItem.castData();
      for (final byte[] byteStringSegment : byteStringList) {
        byteStringLength += byteStringSegment.length;
      }
    }

    // Catch unknown data types.
    else {
      throw new IllegalArgumentException("Source data item must be either a text string or text string list.");
    }
    return byteStringLength;
  }

  /*
   * Extract a single byte array from either a standard byte array data item or
   * concatenated list.
   */
  private static byte[] getByteString(final DataItem<?> sourceDataItem) {
    byte[] byteString;

    // Extract a single byte string.
    if (sourceDataItem.getDataType() == UserDataType.BYTE_STRING) {
      byteString = (byte[]) sourceDataItem.getData();
    }

    // Append data from byte string list.
    else if (sourceDataItem.getDataType() == UserDataType.BYTE_STRING_LIST) {
      final ByteArrayOutputStream byteArrayBuilder = new ByteArrayOutputStream();
      final List<byte[]> byteStringList = sourceDataItem.castData();
      for (final byte[] byteStringSegment : byteStringList) {
        try {
          byteArrayBuilder.write(byteStringSegment);
        } catch (final IOException error) {
          throw new RuntimeException("Unexpected I/O error when writing to local byte array.", error);
        }
      }
      byteString = byteArrayBuilder.toByteArray();
    }

    // Catch unknown data types.
    else {
      throw new IllegalArgumentException("Source data item must be either a text string or text string list.");
    }
    return byteString;
  }

  /*
   * Performs automatic decoding to the byte string format for both expansion and
   * tokenisation.
   */
  private static DataItem<?> performAutoDecoding(final DataItemFactory dataItemFactory,
      final DataItem<?> sourceDataItem, final DecodeStatus defaultStatus, final Logger logger,
      final String loggerPath) {

    // If the source data item is already in binary format, pass the data value
    // through as-is.
    if ((sourceDataItem.getDataType() == UserDataType.BYTE_STRING)
        || (sourceDataItem.getDataType() == UserDataType.BYTE_STRING_LIST)) {
      return sourceDataItem;
    }

    // Extract the encoded string from text based data items.
    String encodedString;
    if (sourceDataItem.getDataType() == UserDataType.TEXT_STRING) {
      encodedString = (String) sourceDataItem.getData();
    } else if (sourceDataItem.getDataType() == UserDataType.TEXT_STRING_LIST) {
      final List<String> encodedStringList = sourceDataItem.castData();
      final StringBuilder stringBuilder = new StringBuilder();
      for (final String encodedStringSegment : encodedStringList) {
        stringBuilder.append(encodedStringSegment);
      }
      encodedString = stringBuilder.toString();
    } else {
      encodedString = null;
    }

    // Create byte array data item from the encoded string, checking for
    // successful decoding on completion.
    DataItem<?> decodedDataItem;
    if (encodedString != null) {
      decodedDataItem = dataItemFactory.createByteStringItem(encodedString, sourceDataItem.getTags())
          .setDecodeStatus(defaultStatus);
      if (decodedDataItem.getDecodeStatus().isFailure()) {
        if (logger != null) {
          logger.warning(loggerPath + " : Base64 decoding failed for supplied text string.");
        }
        decodedDataItem = dataItemFactory.createInvalidItem(decodedDataItem.getDecodeStatus());
      }
    } else {
      if (logger != null) {
        logger.warning(loggerPath + " : No valid data type detected during byte string decoding.");
      }
      decodedDataItem = dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    }
    return decodedDataItem;
  }
}
