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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DataItemFactory;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.InvalidSchemaException;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Extends generic CBOR data schema nodes for use with structure data types.
 *
 * @author Chris Holgate
 */
final class StructureSchemaNode extends SchemaNodeCore {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  // Ordered list of schema records for the data structure.
  private final SchemaNodeCore[] schemaRecords;

  // Specifies whether the defined schema should be treated as final.
  private final boolean schemaFinal;

  /**
   * Provides private default constructor which associates the schema node with a
   * given CBOR data item factory.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param schemaRecords This is the set of schema records which are encapsulated
   *   by the CBOR structure schema.
   * @param schemaFinal Specifies whether the defined schema should be treated as
   *   final.
   */
  private StructureSchemaNode(final DataItemFactory dataItemFactory, final SchemaNodeCore[] schemaRecords,
      final boolean schemaFinal) {
    this.dataItemFactory = dataItemFactory;
    this.schemaRecords = schemaRecords;
    this.schemaFinal = schemaFinal;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new structure schema node.
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
  static StructureSchemaNode build(final DataItemFactory dataItemFactory, final SchemaBuilder schemaBuilder,
      final Map<String, DataItem<?>> schemaMap, final Map<String, SchemaNodeCore> schemaDefinitions,
      final String schemaPath) throws InvalidSchemaException {

    // Extract the boolean 'final' flag.
    final DataItem<?> schemaFinalItem = schemaMap.get("final");
    boolean schemaFinal = false;
    if (schemaFinalItem != null) {
      if (schemaFinalItem.getDataType() != UserDataType.BOOLEAN) {
        throw new InvalidSchemaException(schemaPath + " : CBOR structure schema 'final' field not valid.");
      }
      schemaFinal = (Boolean) schemaFinalItem.getData();
    }

    // Extract the set of structure record definitions.
    final DataItem<?> recordMapItem = schemaMap.get("records");
    if ((recordMapItem == null) || (recordMapItem.getDataType() != UserDataType.NAMED_MAP)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR structure schema 'records' field not valid.");
    }
    final Map<String, DataItem<?>> sourceRecordMap = recordMapItem.castData();

    // Create an array for storing the ordered records.
    final SchemaNodeCore[] schemaRecords = new SchemaNodeCore[sourceRecordMap.size()];
    final Map<String, SchemaNodeCore> schemaRecordMap = new HashMap<String, SchemaNodeCore>(sourceRecordMap.size());

    // Extract all structure records in turn.
    for (final Map.Entry<String, DataItem<?>> sourceMapEntry : sourceRecordMap.entrySet()) {
      final String recordName = sourceMapEntry.getKey();
      final DataItem<?> recordItem = sourceMapEntry.getValue();
      if ((recordItem == null) || (recordItem.getDataType() != UserDataType.NAMED_MAP)) {
        throw new InvalidSchemaException(
            schemaPath + " : CBOR structure schema record '" + recordName + "' is not valid.");
      }

      // Determine the record index value and check that it is both unique and
      // in the valid range.
      final Map<String, DataItem<?>> recordItemMap = recordItem.castData();
      final DataItem<?> indexItem = recordItemMap.get("index");
      if ((indexItem == null) || (indexItem.getDataType() != UserDataType.INTEGER)) {
        throw new InvalidSchemaException(
            schemaPath + " : CBOR structure schema record '" + recordName + "' has no valid 'index' field.");
      }
      final int recordIndex = ((Long) indexItem.getData()).intValue();
      if ((recordIndex < 0) || (recordIndex >= schemaRecords.length) || (schemaRecords[recordIndex] != null)) {
        throw new InvalidSchemaException(schemaPath + " : CBOR structure schema record '" + recordName
            + "' has duplicate or out of range 'index' field value.");
      }

      // Determine if the record required flag is present and set to a
      // legitimate boolean value.
      boolean requiredFlag = false;
      final DataItem<?> requiredItem = recordItemMap.get("required");
      if (requiredItem != null) {
        if (requiredItem.getDataType() != UserDataType.BOOLEAN) {
          throw new InvalidSchemaException(
              schemaPath + " : CBOR structure schema record '" + recordName + "' has invalid 'required' field.");
        }
        requiredFlag = ((Boolean) requiredItem.getData()).booleanValue();
      }

      // Perform recursive processing on the record item to generate the
      // corresponding schema node and then insert it into the indexed array.
      final SchemaNodeCore recordSchema = schemaBuilder.recursiveBuild(recordItemMap, schemaDefinitions,
          schemaPath + ".records." + recordName);
      recordSchema.setName(recordName);
      recordSchema.setTokenValue(Long.valueOf(recordIndex));
      recordSchema.setIsOptional(!requiredFlag);
      schemaRecords[recordIndex] = recordSchema;
      schemaRecordMap.put(recordName, recordSchema);
    }

    // Build the structure data schema.
    return new StructureSchemaNode(dataItemFactory, schemaRecords, schemaFinal);
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.STRUCTURE;
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  StructureSchemaNode duplicate() {
    final StructureSchemaNode duplicatedSchema = new StructureSchemaNode(dataItemFactory, schemaRecords, schemaFinal);
    duplicatedSchema.copyParamsFrom(this);
    return duplicatedSchema;
  }

  /*
   * Implements SchemaNodeCore.createDefault(...)
   */
  @Override
  DataItem<Map<String, DataItem<?>>> createDefault(final boolean includeAll) {
    final DataItem<Map<String, DataItem<?>>> structureDataItem = dataItemFactory.createNamedMapItem(getTagValues(),
        false);
    for (final SchemaNodeCore schemaRecord : schemaRecords) {
      if ((includeAll) || (!schemaRecord.isOptional())) {
        final DataItem<?> recordDataItem = schemaRecord.createDefault(includeAll);
        structureDataItem.getData().put(schemaRecord.getName(), recordDataItem);
      }
    }
    return structureDataItem;
  }

  /*
   * Implements SchemaNodeCore.validate(...)
   */
  @Override
  boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized, final boolean doRecursive,
      final Logger logger, final String loggerPath) {

    // Carry out validation on the tokenized form.
    if (isTokenized) {
      if (!doTokenizedTypeCheck(sourceDataItem, logger, loggerPath)) {
        return false;
      }

      // Perform validation on the array entries.
      final List<DataItem<?>> dataItemList = sourceDataItem.castData();
      final Iterator<DataItem<?>> dataItemIterator = dataItemList.iterator();
      for (int index = 0; index < schemaRecords.length; index++) {

        // For interworking with previous versions of the schema which may not
        // include all the defined records, missing entries at the end of the
        // array are treated as being null.
        final DataItem<?> recordDataItem = (index < dataItemList.size()) ? dataItemIterator.next()
            : dataItemFactory.createUndefinedItem(true, null);

        // Optional entries may be set to 'null' or 'undefined'.
        if ((recordDataItem.getDataType() == UserDataType.NULL)
            || (recordDataItem.getDataType() == UserDataType.UNDEFINED)) {
          if (!schemaRecords[index].isOptional()) {
            if (logger != null) {
              logger.warning(loggerPath + " : Validation failed because a required record was not present.");
            }
            return false;
          }
        }

        // Records are only validated if recursive validation has been selected.
        else {
          if (doRecursive) {
            final String loggerRecordPath = (loggerPath == null) ? null
                : loggerPath + "." + schemaRecords[index].getName();
            if (!schemaRecords[index].validate(recordDataItem, isTokenized, true, logger, loggerRecordPath)) {
              return false;
            }
          }
        }
      }
    }

    // Carry out validation of the expanded form.
    else {
      if (sourceDataItem.getDataType() != UserDataType.NAMED_MAP) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed because source data is not a named map.");
        }
        return false;
      }

      // Perform validation of the map entries.
      int recordCount = 0;
      final Map<String, DataItem<?>> dataItemMap = sourceDataItem.castData();
      for (int index = 0; index < schemaRecords.length; index++) {
        final DataItem<?> recordDataItem = dataItemMap.get(schemaRecords[index].getName());

        // Optional entries can be absent from the map.
        if (recordDataItem == null) {
          if (!schemaRecords[index].isOptional()) {
            if (logger != null) {
              logger.warning(loggerPath + " : Validation failed because a required record was not present.");
            }
            return false;
          }
        }

        // Records are only validated if recursive validation has been selected.
        else {
          if (doRecursive) {
            final String loggerRecordPath = (loggerPath == null) ? null
                : loggerPath + "." + schemaRecords[index].getName();
            if (!schemaRecords[index].validate(recordDataItem, isTokenized, true, logger, loggerRecordPath)) {
              return false;
            }
          }
          recordCount += 1;
        }
      }

      // Check that all records have been processed for 'final' structures.
      if (schemaFinal && (recordCount != dataItemMap.size())) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed on undefined records in 'final' structure.");
        }
        return false;
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

    // Convert the entries in the data item list into a named map of expanded
    // items. Performs implicit validation checks in the loop.
    final List<DataItem<?>> dataItemList = sourceDataItem.castData();
    final Iterator<DataItem<?>> dataItemIterator = dataItemList.iterator();
    final DataItem<Map<String, DataItem<?>>> expandedDataItemMap = dataItemFactory
        .createNamedMapItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.EXPANDED);
    for (int index = 0; index < schemaRecords.length; index++) {

      // For interworking with previous versions of the schema which may not
      // include all the defined records, missing entries at the end of the
      // array are treated as being null.
      final DataItem<?> originalDataItem = (index < dataItemList.size()) ? dataItemIterator.next()
          : dataItemFactory.createUndefinedItem(true, null);

      // Expand the record data prior to adding it to the data item map. Allow
      // null and undefined references for optional records. These are omitted
      // from the expanded map.
      if ((originalDataItem.getDataType() != UserDataType.NULL)
          && (originalDataItem.getDataType() != UserDataType.UNDEFINED)) {
        final String loggerRecordPath = (loggerPath == null) ? null : loggerPath + "." + schemaRecords[index].getName();
        final DataItem<?> expandedDataItem = schemaRecords[index].expand(originalDataItem, logger, loggerRecordPath);
        if (expandedDataItem.getDecodeStatus().isFailure()) {
          return expandedDataItem;
        }
        expandedDataItemMap.getData().put(schemaRecords[index].getName(), expandedDataItem);
      }
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

    // Convert the entries in the named data item map into an ordered list of
    // tokenized items. Performs implicit validation checks in the loop.
    final DataItem<List<DataItem<?>>> tokenizedDataItemList = dataItemFactory
        .createArrayItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.TOKENIZED);
    final Map<String, DataItem<?>> dataItemMap = sourceDataItem.castData();
    for (int index = 0; index < schemaRecords.length; index++) {
      final DataItem<?> originalDataItem = dataItemMap.get(schemaRecords[index].getName());

      // In not present, optional data items can be replaced by undefined
      // references.
      DataItem<?> tokenizedDataItem;
      if ((originalDataItem == null) || (originalDataItem.getDataType() == UserDataType.NULL)
          || (originalDataItem.getDataType() == UserDataType.UNDEFINED)) {
        tokenizedDataItem = dataItemFactory.createUndefinedItem(false, null);
      }

      // Tokenize the record data prior to adding it to the data item list.
      else {
        final String loggerRecordPath = (loggerPath == null) ? null : loggerPath + "." + schemaRecords[index].getName();
        tokenizedDataItem = schemaRecords[index].tokenize(originalDataItem, logger, loggerRecordPath);
        if (tokenizedDataItem.getDecodeStatus().isFailure()) {
          return tokenizedDataItem;
        }
      }
      tokenizedDataItemList.getData().add(tokenizedDataItem);
    }
    return tokenizedDataItemList;
  }

  /*
   * Perform basic type checking on tokenized format. Checks that the source data
   * item is an array of the correct length. Takes into account whether the
   * structure is 'final'.
   */
  private boolean doTokenizedTypeCheck(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {
    if (sourceDataItem.getDataType() != UserDataType.ARRAY) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because source data is not an array.");
      }
      return false;
    }
    final List<DataItem<?>> dataItemList = sourceDataItem.castData();

    // Perform exact length checking for final schema definitions.
    if (schemaFinal) {
      if (dataItemList.size() != schemaRecords.length) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed on incorrect array length for 'final' structure schema.");
        }
        return false;
      }
    }

    // Perform minimum length checking for extensible schema definitions.
    else {
      if (dataItemList.size() < schemaRecords.length) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed on incorrect array length for structure schema.");
        }
        return false;
      }
    }
    return true;
  }
}
