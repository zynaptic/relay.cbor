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
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DataItemFactory;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.InvalidSchemaException;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Extends generic CBOR data schema nodes for use with map data types.
 *
 * @author Chris Holgate
 */
final class MapSchemaNode extends SchemaNodeCore {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  // Specify schema to be used for all array entries.
  private final SchemaNodeCore valuesSchema;

  /**
   * Provides private default constructor which associates the schema node with a
   * given CBOR data item factory.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param valuesSchema This is the schema to be used for array entry items.
   */
  private MapSchemaNode(final DataItemFactory dataItemFactory, final SchemaNodeCore valuesSchema) {
    this.dataItemFactory = dataItemFactory;
    this.valuesSchema = valuesSchema;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new map schema node.
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
  static MapSchemaNode build(final DataItemFactory dataItemFactory, final SchemaBuilder schemaBuilder,
      final Map<String, DataItem<?>> schemaMap, final Map<String, SchemaNodeCore> schemaDefinitions,
      final String schemaPath) throws InvalidSchemaException {

    // Extract the schema definition which is used for all map entries.
    final DataItem<?> mapEntriesItem = schemaMap.get("entries");
    if ((mapEntriesItem == null) || (mapEntriesItem.getDataType() != UserDataType.NAMED_MAP)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR map schema 'entries' field not valid.");
    }
    final Map<String, DataItem<?>> mapEntriesItemMap = mapEntriesItem.castData();

    // Perform recursive processing on the map entries item definition to
    // generate the corresponding schema node.
    final SchemaNodeCore mapEntriesSchema = schemaBuilder.recursiveBuild(mapEntriesItemMap, schemaDefinitions,
        schemaPath + ".entries");

    // Build the map data schema.
    return new MapSchemaNode(dataItemFactory, mapEntriesSchema);
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.MAP;
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  SchemaNodeCore duplicate() {
    final MapSchemaNode duplicatedSchema = new MapSchemaNode(dataItemFactory, valuesSchema);
    duplicatedSchema.copyParamsFrom(this);
    return duplicatedSchema;
  }

  /*
   * Implements SchemaNodeCore.createDefault(...)
   */
  @Override
  DataItem<Map<String, DataItem<?>>> createDefault(final boolean includeAll) {

    // The default is always an empty map using the indefinite length form for
    // CBOR encoding.
    return dataItemFactory.createNamedMapItem(getTagValues(), false);
  }

  /*
   * Implements SchemaNodeCore.validate(...)
   */
  @Override
  boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized, final boolean doRecursive,
      final Logger logger, final String loggerPath) {

    // Check that the source data item is a map.
    if (sourceDataItem.getDataType() != UserDataType.NAMED_MAP) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because source data is not a named map.");
      }
      return false;
    }

    // Perform recursive validation on each of the list entries.
    if (doRecursive) {
      final Map<String, DataItem<?>> sourceDataMap = sourceDataItem.castData();
      for (final Entry<String, DataItem<?>> sourceDataMapEntry : sourceDataMap.entrySet()) {
        final String loggerEntryPath = (loggerPath == null) ? null : loggerPath + "." + sourceDataMapEntry.getKey();
        if (!valuesSchema.validate(sourceDataMapEntry.getValue(), isTokenized, true, logger, loggerEntryPath)) {
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

    // Perform schema expansion on each of the map entries while preserving the
    // key strings.
    final Map<String, DataItem<?>> sourceDataMap = sourceDataItem.castData();
    final DataItem<Map<String, DataItem<?>>> expandedDataItemMap = dataItemFactory
        .createNamedMapItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.EXPANDED);
    for (final Map.Entry<String, DataItem<?>> originalMapEntry : sourceDataMap.entrySet()) {
      final String loggerEntryPath = (loggerPath == null) ? null : loggerPath + "." + originalMapEntry.getKey();
      final DataItem<?> expandedDataItem = valuesSchema.expand(originalMapEntry.getValue(), logger, loggerEntryPath);
      if (expandedDataItem.getDecodeStatus().isFailure()) {
        return expandedDataItem;
      }
      expandedDataItemMap.getData().put(originalMapEntry.getKey(), expandedDataItem);
    }
    return expandedDataItemMap;
  }

  /*
   * Implements SchemaNodeCore.tokenize(...)
   */
  @Override
  DataItem<?> tokenize(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {
    if (!validate(sourceDataItem, false, false, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    }

    // Perform schema tokenization on each of the map entries while preserving
    // the key strings.
    final Map<String, DataItem<?>> sourceDataMap = sourceDataItem.castData();
    final DataItem<Map<String, DataItem<?>>> tokenizedDataItemMap = dataItemFactory
        .createNamedMapItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.TOKENIZED);
    for (final Map.Entry<String, DataItem<?>> originalMapEntry : sourceDataMap.entrySet()) {
      final String loggerEntryPath = (loggerPath == null) ? null : loggerPath + "." + originalMapEntry.getKey();
      final DataItem<?> tokenizedDataItem = valuesSchema.tokenize(originalMapEntry.getValue(), logger, loggerEntryPath);
      if (tokenizedDataItem.getDecodeStatus().isFailure()) {
        return tokenizedDataItem;
      }
      tokenizedDataItemMap.getData().put(originalMapEntry.getKey(), tokenizedDataItem);
    }
    return tokenizedDataItemMap;
  }
}
