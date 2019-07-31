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

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DataItemFactory;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.InvalidSchemaException;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Extends generic CBOR data schema nodes for use with the standard text string
 * data type.
 *
 * @author Chris Holgate
 */
final class TextStringSchemaNode extends SchemaNodeCore {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  // Specifies the default value associated with the schema node.
  private final String defaultValue;

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
   * @param defaultValue This is the default value associated with the text string
   *   node.
   * @param maxLength This specifies the maximum number of bytes which may be
   *   included in the UTF-8 encoded text string (inclusive).
   * @param minLength This specifies the minimum number of bytes which may be
   *   included in the UTF-8 encoded text string (inclusive).
   */
  private TextStringSchemaNode(final DataItemFactory dataItemFactory, final String defaultValue, final int maxLength,
      final int minLength) {
    this.dataItemFactory = dataItemFactory;
    this.defaultValue = defaultValue;
    this.maxLength = maxLength;
    this.minLength = minLength;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new text string schema node.
   *
   * @param cborBaseService This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param schemaMap This is the attribute map which is to be processed in order
   *   to generate the schema node.
   * @param schemaPath This is the hierarchical path to the schema node to be used
   *   for error reporting.
   */
  static TextStringSchemaNode build(final DataItemFactory dataItemFactory, final Map<String, DataItem<?>> schemaMap,
      final String schemaPath) throws InvalidSchemaException {

    // Extract the default value, if present.
    String defaultValue = "";
    int defaultLength = 0;
    final DataItem<?> defaultItem = schemaMap.get("default");
    if (defaultItem != null) {
      if ((defaultItem.getDataType() != UserDataType.TEXT_STRING)
          && (defaultItem.getDataType() != UserDataType.TEXT_STRING_LIST)) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'default' field not valid.");
      }
      defaultValue = getTextString(defaultItem);
      defaultLength = getTextStringLength(defaultItem);
    }

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
    return new TextStringSchemaNode(dataItemFactory, defaultValue, (int) maxLength, (int) minLength);
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.TEXT_STRING;
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  TextStringSchemaNode duplicate() {
    final TextStringSchemaNode duplicatedSchema = new TextStringSchemaNode(dataItemFactory, defaultValue, maxLength,
        minLength);
    duplicatedSchema.copyParamsFrom(this);
    return duplicatedSchema;
  }

  /*
   * Implements SchemaNodeCore.createDefault(...)
   */
  @Override
  DataItem<String> createDefault(final boolean includeAll) {
    return dataItemFactory.createTextStringItem(defaultValue, getTagValues());
  }

  /*
   * Implements SchemaNodeCore.validate(...)
   */
  @Override
  boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized, final boolean doRecursive,
      final Logger logger, final String loggerPath) {
    if ((sourceDataItem.getDataType() != UserDataType.TEXT_STRING)
        && (sourceDataItem.getDataType() != UserDataType.TEXT_STRING_LIST)) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because the source data was not a valid text string.");
      }
      return false;
    }

    // Check the specified text string value against the valid length range.
    final int sourceLength = getTextStringLength(sourceDataItem);
    if ((sourceLength < minLength) || (sourceLength > maxLength)) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because text string length " + sourceLength
            + " is not in valid range " + minLength + " to " + maxLength + ".");
      }
      return false;
    }
    return true;
  }

  /*
   * Implements SchemaNodeCore.expand(...)
   */
  @Override
  DataItem<?> expand(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {

    // Performs data type checking before passing the data value through as-is.
    if (!validate(sourceDataItem, true, false, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    }
    return sourceDataItem;
  }

  /*
   * Implements SchemaNodeCore.tokenize(...)
   */
  @Override
  DataItem<?> tokenize(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {

    // Performs data type checking before passing the data value through as-is.
    if (!validate(sourceDataItem, false, false, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    }
    return sourceDataItem;
  }

  /*
   * Extract a single string from either a standard string data item or
   * concatenated string list.
   */
  private static String getTextString(final DataItem<?> sourceDataItem) {
    String textString;

    // Extract a single string.
    if (sourceDataItem.getDataType() == UserDataType.TEXT_STRING) {
      textString = (String) sourceDataItem.getData();
    }

    // Append data from string list.
    else if (sourceDataItem.getDataType() == UserDataType.TEXT_STRING_LIST) {
      final StringBuilder stringBuilder = new StringBuilder();
      final List<String> textStringList = sourceDataItem.castData();
      for (final String textStringSegment : textStringList) {
        stringBuilder.append(textStringSegment);
      }
      textString = stringBuilder.toString();
    }

    // Catch unknown data types.
    else {
      throw new IllegalArgumentException("Source data item must be either a text string or text string list.");
    }
    return textString;
  }

  /*
   * Count the number of bytes used to represent a given string data item or
   * concatenated string list.
   */
  private static int getTextStringLength(final DataItem<?> sourceDataItem) {
    int stringLength;
    try {

      // Count UTF-8 bytes in a single string.
      if (sourceDataItem.getDataType() == UserDataType.TEXT_STRING) {
        stringLength = ((String) sourceDataItem.getData()).getBytes("UTF-8").length;
      }

      // Add together UTF-8 bytes in string list.
      else if (sourceDataItem.getDataType() == UserDataType.TEXT_STRING_LIST) {
        stringLength = 0;
        final List<String> textStringList = sourceDataItem.castData();
        for (final String textStringSegment : textStringList) {
          stringLength += textStringSegment.getBytes("UTF-8").length;
        }
      }

      // Catch unknown data types.
      else {
        throw new IllegalArgumentException("Source data item must be either a text string or text string list.");
      }
    } catch (final UnsupportedEncodingException error) {
      throw new RuntimeException("UTF-8 string encoding should always be supported by JVM.", error);
    }
    return stringLength;
  }
}
