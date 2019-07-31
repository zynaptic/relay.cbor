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
 * Extends generic CBOR data schema nodes for use with floating point value data
 * types.
 *
 * @author Chris Holgate
 */
final class NumberSchemaNode extends SchemaNodeCore {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  // Specifies the precision to be used for representing the number in CBOR
  // tokenized form.
  private final UserDataType precision;

  // Specifies the default value associated with the schema node.
  private final double defaultValue;

  // Specifies the maximum permissible value associated with the schema node.
  private final double maxValue;

  // Specifies the minimum permissible value associated with the schema node.
  private final double minValue;

  // Specifies whether the maximum value limit is exclusive.
  private final boolean excludeMax;

  // Specifies whether the minimum value limit is exclusive.
  private final boolean excludeMin;

  /**
   * Provides private default constructor which associates the schema node with a
   * given CBOR data item factory.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param precision This is the user data type which specifies the CBOR floating
   *   point type to be used when representing the floating point value.
   * @param defaultValue Specifies the default value associated with the schema
   *   node.
   * @param maxValue Specifies the maximum permissible value associated with the
   *   schema node.
   * @param minValue Specifies the minimum permissible value associated with the
   *   schema node.
   * @param excludeMax Specifies whether the maximum value limit is exclusive.
   * @param excludeMin Specifies whether the minimum value limit is exclusive.
   */
  private NumberSchemaNode(final DataItemFactory dataItemFactory, final UserDataType precision,
      final double defaultValue, final double maxValue, final double minValue, final boolean excludeMax,
      final boolean excludeMin) {
    this.dataItemFactory = dataItemFactory;
    this.precision = precision;
    this.defaultValue = defaultValue;
    this.maxValue = maxValue;
    this.minValue = minValue;
    this.excludeMax = excludeMax;
    this.excludeMin = excludeMin;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new floating point number schema node.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param schemaMap This is the attribute map which is to be processed in order
   *   to generate the schema node.
   * @param schemaPath This is the hierarchical path to the schema node to be used
   *   for error reporting.
   */
  static NumberSchemaNode build(final DataItemFactory dataItemFactory, final Map<String, DataItem<?>> schemaMap,
      final String schemaPath) throws InvalidSchemaException {

    // Extract the precision term if present, defaulting to double precision if
    // not specified.
    UserDataType floatDataType;
    double maxPrecisionValue;
    final DataItem<?> precisionItem = schemaMap.get("precision");
    if (precisionItem == null) {
      floatDataType = UserDataType.FLOAT_DOUBLE;
      maxPrecisionValue = Double.MAX_VALUE;
    } else {
      if (precisionItem.getDataType() != UserDataType.TEXT_STRING) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'precision' field not valid.");
      }
      final String precisionString = (String) precisionItem.getData();
      switch (precisionString) {
      case "half":
        floatDataType = UserDataType.FLOAT_HALF;
        maxPrecisionValue = MAX_HALF_PRECISION_FLOAT;
        break;
      case "standard":
        floatDataType = UserDataType.FLOAT_STANDARD;
        maxPrecisionValue = Float.MAX_VALUE;
        break;
      case "double":
        floatDataType = UserDataType.FLOAT_DOUBLE;
        maxPrecisionValue = Double.MAX_VALUE;
        break;
      default:
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'precision' field not valid.");
      }
    }

    // Extract the default value, if present.
    double defaultValue = 0;
    final DataItem<?> defaultItem = schemaMap.get("default");
    if (defaultItem != null) {
      defaultValue = convertDataItem(defaultItem);
      if (Double.isInfinite(defaultValue) || Double.isNaN(defaultValue)) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'default' field not valid.");
      }
    }

    // Extract the maximum value limit which is associated with the schema node,
    // taking into account the excludeMax flag if required.
    double maxValue = maxPrecisionValue;
    boolean excludeMax = false;
    final DataItem<?> maxValueItem = schemaMap.get("maxValue");
    if (maxValueItem != null) {
      maxValue = convertDataItem(maxValueItem);
      if (Double.isInfinite(maxValue) || Double.isNaN(maxValue)) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'maxValue' field not valid.");
      }

      final DataItem<?> excludeMaxItem = schemaMap.get("excludeMax");
      if (excludeMaxItem != null) {
        if (excludeMaxItem.getDataType() != UserDataType.BOOLEAN) {
          throw new InvalidSchemaException(schemaPath + " : CBOR schema 'excludeMax' field not valid.");
        }
        excludeMax = ((Boolean) excludeMaxItem.getData());
      }
    }

    // Extract the minimum value limit which is associated with the schema node,
    // taking into account the excludeMin flag if required.
    double minValue = -maxPrecisionValue;
    boolean excludeMin = false;
    final DataItem<?> minValueItem = schemaMap.get("minValue");
    if (minValueItem != null) {
      minValue = convertDataItem(minValueItem);
      if (Double.isInfinite(minValue) || Double.isNaN(minValue)) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'minValue' field not valid.");
      }

      final DataItem<?> excludeMinItem = schemaMap.get("excludeMin");
      if (excludeMinItem != null) {
        if (excludeMinItem.getDataType() != UserDataType.BOOLEAN) {
          throw new InvalidSchemaException(schemaPath + " : CBOR schema 'excludeMin' field not valid.");
        }
        excludeMin = ((Boolean) excludeMinItem.getData());
      }
    }

    // Perform sanity checking on the supplied range parameters.
    if (maxValue > maxPrecisionValue) {
      throw new InvalidSchemaException(
          schemaPath + " : CBOR schema 'maxValue' field exceeds maximum number representation.");
    }
    if (minValue < -maxPrecisionValue) {
      throw new InvalidSchemaException(
          schemaPath + " : CBOR schema 'minValue' field exceeds minimum number representation.");
    }
    if (maxValue < minValue) {
      throw new InvalidSchemaException(schemaPath + " : CBOR schema 'minValue' and 'maxValue' define an empty range.");
    }
    if ((excludeMin & (defaultValue <= minValue)) || (!excludeMin & (defaultValue < minValue))
        || (excludeMax & (defaultValue >= maxValue)) || (!excludeMax & (defaultValue > maxValue))) {
      throw new InvalidSchemaException(schemaPath + " : CBOR schema 'default' value falls outside defined range.");
    }

    // Build the floating point schema.
    return new NumberSchemaNode(dataItemFactory, floatDataType, defaultValue, maxValue, minValue, excludeMax,
        excludeMin);
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.NUMBER;
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  NumberSchemaNode duplicate() {
    final NumberSchemaNode duplicatedSchema = new NumberSchemaNode(dataItemFactory, precision, defaultValue, maxValue,
        minValue, excludeMax, excludeMin);
    duplicatedSchema.copyParamsFrom(this);
    return duplicatedSchema;
  }

  /*
   * Implements SchemaNodeCore.createDefault(...)
   */
  @Override
  DataItem<?> createDefault(final boolean includeAll) {
    switch (precision) {
    case FLOAT_HALF:
      return dataItemFactory.createHpFloatItem((float) defaultValue, getTagValues());
    case FLOAT_STANDARD:
      return dataItemFactory.createSpFloatItem((float) defaultValue, getTagValues());
    default:
      return dataItemFactory.createDpFloatItem(defaultValue, getTagValues());
    }
  }

  /*
   * Implements SchemaNodeCore.validate(...)
   */
  @Override
  boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized, final boolean doRecursive,
      final Logger logger, final String loggerPath) {

    // Convert any valid number type to double precision and perform range
    // checking. Fails if this does not result in a finite value.
    final double validatedValue = rangeCheckDataItem(sourceDataItem, logger, loggerPath);
    return (!(Double.isInfinite(validatedValue) || Double.isNaN(validatedValue)));
  }

  /*
   * Implements SchemaNodeCore.expand(...)
   */
  @Override
  DataItem<?> expand(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {

    // Check for valid type and perform range checking, converting all numeric
    // values to double precision. This does not verify that the source data
    // format matches the expected precision.
    final double expandedValue = rangeCheckDataItem(sourceDataItem, logger, loggerPath);

    // Non-finite representations are not supported in JSON and constitute a
    // schema failure.
    if (Double.isInfinite(expandedValue) || Double.isNaN(expandedValue)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    } else {
      return dataItemFactory.createDpFloatItem(expandedValue, sourceDataItem.getTags());
    }
  }

  /*
   * Implements SchemaNodeCore.tokenize(...)
   */
  @Override
  DataItem<?> tokenize(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {

    // Check for valid type and perform range checking, converting all numeric
    // values to double precision. This does not verify that the source data
    // format matches the expected precision.
    final double tokenizedValue = rangeCheckDataItem(sourceDataItem, logger, loggerPath);

    // Non-finite representations are not supported in JSON and constitute a
    // schema failure.
    if (Double.isInfinite(tokenizedValue) || Double.isNaN(tokenizedValue)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    }

    // Convert to specified precision. Defaults to double precision if not
    // explicitly specified.
    DataItem<?> floatDataItem;
    switch (precision) {
    case FLOAT_HALF:
      floatDataItem = dataItemFactory.createHpFloatItem((float) tokenizedValue, sourceDataItem.getTags());
      break;
    case FLOAT_STANDARD:
      floatDataItem = dataItemFactory.createSpFloatItem((float) tokenizedValue, sourceDataItem.getTags());
      break;
    default:
      floatDataItem = dataItemFactory.createDpFloatItem(tokenizedValue, sourceDataItem.getTags());
      break;
    }
    return floatDataItem;
  }

  /*
   * Utility function for converting an arbitrary numeric item to double precision
   * floating point and performing range checks against maximum and minimum
   * values.
   */
  private double rangeCheckDataItem(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {
    double dataValue = convertDataItem(sourceDataItem);

    // Unexpected formats map to 'not a number'.
    if (Double.isNaN(dataValue)) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because source data is not a numeric value.");
      }
    }

    // Convert all representations outside the defined range to infinite values.
    // Perform range checking on the source data value.
    else if ((excludeMin & (dataValue <= minValue)) || (!excludeMin & (dataValue < minValue))) {
      dataValue = Double.NEGATIVE_INFINITY;
      if (logger != null) {
        logger.warning(
            loggerPath + " : Validation failed because value " + dataValue + " is less than " + minValue + ".");
      }
    } else if ((excludeMax & (dataValue >= maxValue)) || (!excludeMax & (dataValue > maxValue))) {
      dataValue = Double.POSITIVE_INFINITY;
      if (logger != null) {
        logger.warning(
            loggerPath + " : Validation failed because value " + dataValue + " is greater than " + maxValue + ".");
      }
    }
    return dataValue;
  }

  /*
   * Utility function for converting an arbitrary numeric item to double precision
   * floating point.
   */
  private static double convertDataItem(final DataItem<?> sourceDataItem) {
    double dataValue;

    // Check for valid type, converting all numeric values to double precision.
    switch (sourceDataItem.getDataType()) {
    case FLOAT_HALF:
    case FLOAT_STANDARD:
      dataValue = ((Float) sourceDataItem.getData()).doubleValue();
      break;
    case FLOAT_DOUBLE:
      dataValue = (Double) sourceDataItem.getData();
      break;
    case INTEGER:
      dataValue = ((Long) sourceDataItem.getData()).doubleValue();
      break;
    default:
      dataValue = Double.NaN;
      break;
    }
    return dataValue;
  }
}
