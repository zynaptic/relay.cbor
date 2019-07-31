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

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DataItemFactory;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.InvalidSchemaException;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Extends generic CBOR schema definition nodes for use with array data types.
 *
 * @author Chris Holgate
 */
final class ArraySchemaNode extends SchemaNodeCore {

  // Local reference to the data item factory.
  private final DataItemFactory dataItemFactory;

  // Specify schema to be used for all array entries.
  private final SchemaNodeCore valuesSchema;

  // Specify the minimum permissible size for the array.
  private final int minLength;

  // Specify the maximum permissible size for the array.
  private final int maxLength;

  /**
   * Provides private default constructor which associates the schema node with a
   * given data item factory.
   *
   * @param cborBaseService This is the CBOR base service which is to be used by
   *   the schema node.
   * @param valuesSchema This is the schema to be used for array entry items.
   * @param minLength This is the (inclusive) minimum size for the array, or a
   *   null reference if no minimum size is specified.
   * @param maxLength This is the (inclusive) maximum size for the array, or a
   *   null reference if no maximum size is specified.
   */
  private ArraySchemaNode(final DataItemFactory dataItemFactory, final SchemaNodeCore valuesSchema,
      final Integer minLength, final Integer maxLength) {
    this.dataItemFactory = dataItemFactory;
    this.valuesSchema = valuesSchema;
    this.minLength = minLength;
    this.maxLength = maxLength;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new array schema node.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param schemaBuilder This is a reference to the schema builder object which
   *   is used for recursive schema building.
   * @param schemaMap This is the attribute map which is to be processed in order
   *   to generate the schema node.
   * @param schemaDefinitions This is the set of pre-specified schema definitions
   *   which may be used in place of conventional data types.
   * @param schemaPath This is the hierarchical path to the schema node to be used
   *   for error reporting.
   */
  static ArraySchemaNode build(final DataItemFactory dataItemFactory, final SchemaBuilder schemaBuilder,
      final Map<String, DataItem<?>> schemaMap, final Map<String, SchemaNodeCore> schemaDefinitions,
      final String schemaPath) throws InvalidSchemaException {

    // Extract the schema definition which is used for all array entries.
    final DataItem<?> arrayEntriesItem = schemaMap.get("entries");
    if ((arrayEntriesItem == null) || (arrayEntriesItem.getDataType() != UserDataType.NAMED_MAP)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR array schema 'entries' field not valid.");
    }
    final Map<String, DataItem<?>> arrayEntriesItemMap = arrayEntriesItem.castData();

    // Perform recursive processing on the array entries item definition to
    // generate the corresponding schema node.
    final SchemaNodeCore arrayEntriesSchema = schemaBuilder.recursiveBuild(arrayEntriesItemMap, schemaDefinitions,
        schemaPath + ".entries");

    // Extract the fixed array length option if present.
    long minLength = 0;
    long maxLength = Integer.MAX_VALUE;
    final DataItem<?> fixedLengthItem = schemaMap.get("length");
    if (fixedLengthItem != null) {
      if (fixedLengthItem.getDataType() != UserDataType.INTEGER) {
        throw new InvalidSchemaException(schemaPath + " : CBOR array schema 'length' field not valid.");
      }
      minLength = (Long) fixedLengthItem.getData();
      maxLength = minLength;
    }

    // Extract maximum and minimum length options if fixed length option is not
    // present.
    else {
      final DataItem<?> minLengthItem = schemaMap.get("minLength");
      if (minLengthItem != null) {
        if (minLengthItem.getDataType() != UserDataType.INTEGER) {
          throw new InvalidSchemaException(schemaPath + " : CBOR array schema 'minLength' field not valid.");
        }
        minLength = (Long) minLengthItem.getData();
      }
      final DataItem<?> maxLengthItem = schemaMap.get("maxLength");
      if (maxLengthItem != null) {
        if (maxLengthItem.getDataType() != UserDataType.INTEGER) {
          throw new InvalidSchemaException(schemaPath + " : CBOR array schema 'maxLength' field not valid.");
        }
        maxLength = (Long) maxLengthItem.getData();
      }
    }

    // Perform sanity checking on array length fields.
    if ((minLength < 0) || (maxLength < minLength) || (maxLength > Integer.MAX_VALUE)) {
      throw new InvalidSchemaException(
          schemaPath + " : CBOR array schema 'length', 'minLength' or 'maxLength' fields not valid.");
    }

    // Build the array data schema.
    return new ArraySchemaNode(dataItemFactory, arrayEntriesSchema, (int) minLength, (int) maxLength);
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  ArraySchemaNode duplicate() {
    final ArraySchemaNode duplicatedSchema = new ArraySchemaNode(dataItemFactory, valuesSchema, minLength, maxLength);
    duplicatedSchema.copyParamsFrom(this);
    return duplicatedSchema;
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.ARRAY;
  }

  /*
   * Implements SchemaNodeCore.createDefault(...)
   */
  @Override
  DataItem<?> createDefault(final boolean includeAll) {

    // The default is usually an empty array using the indefinite length form
    // for CBOR encoding. If a non-zero minimum length is specified for the
    // array, a minimum length array is created where all entries are set to the
    // default value specified for the values schema.
    final DataItem<List<DataItem<?>>> arrayDataItem = dataItemFactory.createArrayItem(getTagValues(), true);
    for (int index = 0; index < minLength; index++) {
      final DataItem<?> defaultArrayEntry = valuesSchema.createDefault(includeAll);
      arrayDataItem.getData().add(defaultArrayEntry);
    }
    return arrayDataItem;
  }

  /*
   * Implements SchemaNodeCore.validate(...)
   */
  @Override
  boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized, final boolean doRecursive,
      final Logger logger, final String loggerPath) {

    // Check that the source data item is an array of the correct length.
    if (sourceDataItem.getDataType() != UserDataType.ARRAY) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because source data is not an array.");
      }
      return false;
    }
    final List<DataItem<?>> sourceDataList = sourceDataItem.castData();
    if ((sourceDataList.size() < minLength) || (sourceDataList.size() > maxLength)) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because source array length " + sourceDataList.size()
            + " is outside range " + minLength + " to " + maxLength + ".");
      }
      return false;
    }

    // Perform recursive validation on each of the list entries.
    if (doRecursive) {
      int index = 0;
      for (final DataItem<?> sourceDataListEntry : sourceDataList) {
        final String loggerIndexPath = (loggerPath == null) ? null : loggerPath + "[" + (index++) + "]";
        if (!valuesSchema.validate(sourceDataListEntry, isTokenized, true, logger, loggerIndexPath)) {
          return false;
        }
      }
    }
    return true;
  }

  /*
   * Implements SchemaNodeCore.expand(...)
   */
  @Override
  DataItem<?> expand(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {
    if (!validate(sourceDataItem, true, false, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    }

    // Perform schema expansion on each of the list entries.
    final List<DataItem<?>> sourceDataList = sourceDataItem.castData();
    final DataItem<List<DataItem<?>>> expandedDataItemList = dataItemFactory
        .createArrayItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.EXPANDED);
    int index = 0;
    for (final DataItem<?> originalDataItem : sourceDataList) {
      final String loggerIndexPath = (loggerPath == null) ? null : loggerPath + "[" + (index++) + "]";
      final DataItem<?> expandedDataItem = valuesSchema.expand(originalDataItem, logger, loggerIndexPath);
      if (expandedDataItem.getDecodeStatus().isFailure()) {
        return expandedDataItem;
      }
      expandedDataItemList.getData().add(expandedDataItem);
    }
    return expandedDataItemList;
  }

  /*
   * Implements SchemaNodeCore.tokenize(...)
   */
  @Override
  DataItem<?> tokenize(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {
    if (!validate(sourceDataItem, false, false, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    }

    // Perform schema tokenization on each of the list entries.
    final List<DataItem<?>> sourceDataList = sourceDataItem.castData();
    final DataItem<List<DataItem<?>>> tokenizedDataItemList = dataItemFactory
        .createArrayItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.EXPANDED);
    int index = 0;
    for (final DataItem<?> originalDataItem : sourceDataList) {
      final String loggerIndexPath = (loggerPath == null) ? null : loggerPath + "[" + (index++) + "]";
      final DataItem<?> tokenizedDataItem = valuesSchema.expand(originalDataItem, logger, loggerIndexPath);
      if (tokenizedDataItem.getDecodeStatus().isFailure()) {
        return tokenizedDataItem;
      }
      tokenizedDataItemList.getData().add(tokenizedDataItem);
    }
    return tokenizedDataItemList;
  }
}
