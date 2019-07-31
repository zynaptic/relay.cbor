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

import java.util.Map;
import java.util.logging.Logger;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DataItemFactory;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.InvalidSchemaException;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Extends generic CBOR data schema nodes for use with the integer value data
 * type.
 *
 * @author Chris Holgate
 */
final class IntegerSchemaNode extends SchemaNodeCore {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  // Specifies the default value associated with the schema node.
  private final long defaultValue;

  // Specifies the maximum integer value associated with the schema node
  // (inclusive range).
  private final long maxValue;

  // Specifies the minimum integer value associated with the schema node
  // (inclusive range).
  private final long minValue;

  /**
   * Provides private default constructor which associates the schema node with a
   * given CBOR data item factory.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param defaultValue This is the default integer value associated with the
   *   schema node.
   * @param maxValue Specifies the maximum integer value associated with the
   *   schema node (inclusive range).
   * @param minValue Specifies the minimum integer value associated with the
   *   schema node (inclusive range).
   */
  private IntegerSchemaNode(final DataItemFactory dataItemFactory, final long defaultValue, final long maxValue,
      final long minValue) {
    this.dataItemFactory = dataItemFactory;
    this.defaultValue = defaultValue;
    this.maxValue = maxValue;
    this.minValue = minValue;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new integer schema node.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param schemaMap This is the attribute map which is to be processed in order
   *   to generate the schema node.
   * @param schemaPath This is the hierarchical path to the schema node to be used
   *   for error reporting.
   */
  static IntegerSchemaNode build(final DataItemFactory dataItemFactory, final Map<String, DataItem<?>> schemaMap,
      final String schemaPath) throws InvalidSchemaException {

    // Extract the default value, if present.
    long defaultValue = 0;
    final DataItem<?> defaultItem = schemaMap.get("default");
    if (defaultItem != null) {
      if (defaultItem.getDataType() != UserDataType.INTEGER) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'default' field not valid.");
      }
      defaultValue = (Long) defaultItem.getData();
    }

    // Extract the maximum value limit which is associated with the schema node,
    // taking into account the excludeMax flag if required.
    long maxValue = MAX_INTEGER_VALUE;
    final DataItem<?> maxValueItem = schemaMap.get("maxValue");
    if (maxValueItem != null) {
      if (maxValueItem.getDataType() != UserDataType.INTEGER) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'maxValue' field not valid.");
      }
      maxValue = (Long) maxValueItem.getData();

      final DataItem<?> excludeMaxItem = schemaMap.get("excludeMax");
      if (excludeMaxItem != null) {
        if (excludeMaxItem.getDataType() != UserDataType.BOOLEAN) {
          throw new InvalidSchemaException(schemaPath + " : CBOR schema 'excludeMax' field not valid.");
        }
        if ((Boolean) excludeMaxItem.getData()) {
          maxValue -= 1;
        }
      }
    }

    // Extract the minimum value limit which is associated with the schema node,
    // taking into account the excludeMin flag if required.
    long minValue = MIN_INTEGER_VALUE;
    final DataItem<?> minValueItem = schemaMap.get("minValue");
    if (minValueItem != null) {
      if (minValueItem.getDataType() != UserDataType.INTEGER) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'minValue' field not valid.");
      }
      minValue = (Long) minValueItem.getData();

      final DataItem<?> excludeMinItem = schemaMap.get("excludeMin");
      if (excludeMinItem != null) {
        if (excludeMinItem.getDataType() != UserDataType.BOOLEAN) {
          throw new InvalidSchemaException(schemaPath + " : CBOR schema 'excludeMin' field not valid.");
        }
        if ((Boolean) excludeMinItem.getData()) {
          minValue += 1;
        }
      }
    }

    // Perform a sanity check on the specified parameters.
    if (maxValue > MAX_INTEGER_VALUE) {
      throw new InvalidSchemaException(
          schemaPath + " : CBOR schema 'maxValue' field exceeds valid integer maximum of 2^53.");
    }
    if (minValue < MIN_INTEGER_VALUE) {
      throw new InvalidSchemaException(
          schemaPath + " : CBOR schema 'minValue' field exceeds valid integer minimum of -2^53.");
    }
    if (maxValue < minValue) {
      throw new InvalidSchemaException(schemaPath + " : CBOR schema 'minValue' and 'maxValue' define an empty range.");
    }
    if ((defaultValue < minValue) || (defaultValue > maxValue)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR schema 'default' value falls outside defined range.");
    }
    return new IntegerSchemaNode(dataItemFactory, defaultValue, maxValue, minValue);
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.INTEGER;
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  IntegerSchemaNode duplicate() {
    final IntegerSchemaNode duplicatedSchema = new IntegerSchemaNode(dataItemFactory, defaultValue, maxValue, minValue);
    duplicatedSchema.copyParamsFrom(this);
    return duplicatedSchema;
  }

  /*
   * Implements SchemaNodeCore.createDefault(...)
   */
  @Override
  DataItem<Long> createDefault(final boolean includeAll) {
    return dataItemFactory.createIntegerItem(defaultValue, getTagValues());
  }

  /*
   * Implements SchemaNodeCore.validate(...)
   */
  @Override
  boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized, final boolean doRecursive,
      final Logger logger, final String loggerPath) {
    if (sourceDataItem.getDataType() != UserDataType.INTEGER) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because source data is not an integer value.");
      }
      return false;
    }

    // Check the specified integer value against the valid range.
    final long sourceDataValue = (Long) sourceDataItem.getData();
    if ((sourceDataValue < minValue) || (sourceDataValue > maxValue)) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because value " + sourceDataValue + " is outside range "
            + minValue + " to " + maxValue + ".");
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

    // Performs validation checking before passing the data value through as-is.
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

    // Performs validation checking before passing the data value through as-is.
    if (!validate(sourceDataItem, false, false, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    }
    return sourceDataItem;
  }
}
