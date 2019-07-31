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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DataItemFactory;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.InvalidSchemaException;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Extends generic CBOR data schema nodes for use with selection data types.
 *
 * @author Chris Holgate
 */
final class SelectionSchemaNode extends SchemaNodeCore {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  // Map of format identifiers to format definitions.
  private final Map<String, SchemaNodeCore> formatIdentMap;

  // Map of format token values to format definitions.
  private final Map<Long, SchemaNodeCore> formatTokenMap;

  // Specifies whether the defined schema should be treated as final.
  private final boolean schemaFinal;

  // Specifies the default format to be used by the selection data type.
  private final String defaultFormat;

  /**
   * Provides private default constructor which associates the schema node with a
   * given CBOR data item factory.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param formatIdentMap This is the map of supported format identifiers to
   *   their corresponding format schemas.
   * @param formatTokenMap This is the map of supported format tokens to their
   *   corresponding format schemas.
   * @param schemaFinal Specifies whether the defined schema should be treated as
   *   final.
   * @param defaultFormat Specifies the default format to be used by the selection
   *   schema.
   */
  private SelectionSchemaNode(final DataItemFactory dataItemFactory, final Map<String, SchemaNodeCore> formatIdentMap,
      final Map<Long, SchemaNodeCore> formatTokenMap, final boolean schemaFinal, final String defaultFormat) {
    this.dataItemFactory = dataItemFactory;
    this.formatIdentMap = formatIdentMap;
    this.formatTokenMap = formatTokenMap;
    this.schemaFinal = schemaFinal;
    this.defaultFormat = defaultFormat;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new selection schema node.
   *
   * @param cborBaseService This is the CBOR data item factory which is to be used
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
  static SelectionSchemaNode build(final DataItemFactory dataItemFactory, final SchemaBuilder schemaBuilder,
      final Map<String, DataItem<?>> schemaMap, final Map<String, SchemaNodeCore> schemaDefinitions,
      final String schemaPath) throws InvalidSchemaException {

    // Determine if the object is extensible or final.
    boolean schemaFinal;
    final DataItem<?> finalFlagItem = schemaMap.get("final");
    if (finalFlagItem == null) {
      schemaFinal = false;
    } else if (finalFlagItem.getDataType() == UserDataType.BOOLEAN) {
      schemaFinal = (Boolean) finalFlagItem.getData();
    } else {
      throw new InvalidSchemaException(schemaPath + " : CBOR selection schema 'final' field not valid.");
    }

    // Extract the schema definition formats map.
    final DataItem<?> schemaFormatsItem = schemaMap.get("formats");
    if ((schemaFormatsItem == null) || (schemaFormatsItem.getDataType() != UserDataType.NAMED_MAP)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR selection schema 'formats' field not valid.");
    }
    final Map<String, DataItem<?>> schemaFormatsMap = schemaFormatsItem.castData();

    // Process each entry in the schema formats map, including recursively
    // building the associated schemas.
    final Map<String, SchemaNodeCore> formatIdentMap = new HashMap<String, SchemaNodeCore>(schemaFormatsMap.size());
    final Map<Long, SchemaNodeCore> formatTokenMap = new HashMap<Long, SchemaNodeCore>(schemaFormatsMap.size());
    for (final Entry<String, DataItem<?>> schemaFormatsEntry : schemaFormatsMap.entrySet()) {

      // Extract the schema definition map.
      final String formatIdent = schemaFormatsEntry.getKey();
      if (schemaFormatsEntry.getValue().getDataType() != UserDataType.NAMED_MAP) {
        throw new InvalidSchemaException(
            schemaPath + ".formats" + " : CBOR selection invalid schema definition for format '" + formatIdent + "'.");
      }
      final Map<String, DataItem<?>> formatSchemaMap = schemaFormatsEntry.getValue().castData();

      // Extract the token value for the schema definition.
      final DataItem<?> tokenValueItem = formatSchemaMap.get("token");
      if ((tokenValueItem == null) || (tokenValueItem.getDataType() != UserDataType.INTEGER)) {
        throw new InvalidSchemaException(
            schemaPath + ".formats" + " : CBOR selection 'token' field not valid for format '" + formatIdent + "'.");
      }
      final Long tokenValue = (Long) tokenValueItem.getData();

      // Check for use of the reserved selection name.
      if (schemaFormatsEntry.getKey().equals("unknown")) {
        throw new InvalidSchemaException(
            schemaPath + ".formats : CBOR selection format identifier 'unknown' is reserved.");
      }

      // Check for the reserved token value or duplicate token definitions.
      if (tokenValue == 0) {
        throw new InvalidSchemaException(
            schemaPath + ".formats" + " : CBOR selection token value 0 is reserved for format '" + formatIdent + "'.");
      }
      if (formatTokenMap.containsKey(tokenValue)) {
        throw new InvalidSchemaException(
            schemaPath + ".formats" + " : CBOR selection token is a duplicate for format '" + formatIdent + "'.");
      }

      // Recursively generate the format schema and add it to the format maps.
      final SchemaNodeCore formatSchema = schemaBuilder.recursiveBuild(formatSchemaMap, schemaDefinitions,
          schemaPath + ".formats." + formatIdent);
      formatSchema.setName(formatIdent);
      formatSchema.setTokenValue(tokenValue);
      formatIdentMap.put(formatIdent, formatSchema);
      formatTokenMap.put(tokenValue, formatSchema);
    }

    // Extract the default format selection, checking that it is within the
    // defined set.
    final DataItem<?> defaultItem = schemaMap.get("default");
    if ((defaultItem == null) || (defaultItem.getDataType() != UserDataType.TEXT_STRING)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR selection schema 'default' field not valid.");
    }
    final String defaultFormat = (String) defaultItem.getData();
    if (!formatIdentMap.containsKey(defaultFormat)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR selection schema 'default' field not in defined formats.");
    }

    // Build the new selection data schema.
    return new SelectionSchemaNode(dataItemFactory, formatIdentMap, formatTokenMap, schemaFinal, defaultFormat);
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.SELECTION;
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  SelectionSchemaNode duplicate() {
    final SelectionSchemaNode duplicatedSchema = new SelectionSchemaNode(dataItemFactory, formatIdentMap,
        formatTokenMap, schemaFinal, defaultFormat);
    duplicatedSchema.copyParamsFrom(this);
    return duplicatedSchema;
  }

  /*
   * Implements SchemaNodeCore.createDefault(...)
   */
  @Override
  DataItem<Map<String, DataItem<?>>> createDefault(final boolean includeAll) {
    final SchemaNodeCore defaultSchema = formatIdentMap.get(defaultFormat);
    final DataItem<?> defaultDataItem = defaultSchema.createDefault(includeAll);
    final DataItem<Map<String, DataItem<?>>> selectionDataItem = dataItemFactory.createNamedMapItem(getTagValues(),
        false);
    selectionDataItem.getData().put(defaultFormat, defaultDataItem);
    return selectionDataItem;
  }

  /*
   * Implements SchemaNodeCore.validate(...)
   */
  @Override
  boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized, final boolean doRecursive,
      final Logger logger, final String loggerPath) {

    // Perform validation on tokenized form, first checking that the source data
    // item is a two entry array.
    if (isTokenized) {
      if (sourceDataItem.getDataType() != UserDataType.ARRAY) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed because source data is not an array.");
        }
        return false;
      }
      final List<DataItem<?>> dataItemList = sourceDataItem.castData();
      if (dataItemList.size() != 2) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed because source data is not a two entry array.");
        }
        return false;
      }

      // Extract the token which identifies the selection format schema to be
      // used for the data.
      final DataItem<?> tokenItem = dataItemList.get(0);
      if ((tokenItem == null) || (tokenItem.getDataType() != UserDataType.INTEGER)) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed because selection token is not an integer.");
        }
        return false;
      }
      final Long tokenValue = (Long) tokenItem.getData();

      // Process known token values
      final SchemaNodeCore selectedDataSchema = formatTokenMap.get(tokenValue);
      if (selectedDataSchema != null) {
        if (doRecursive) {
          return selectedDataSchema.validate(dataItemList.get(1), isTokenized, true, logger, loggerPath);
        }
      }

      // Fail for unknown token values when using final schema definitions.
      else if (schemaFinal) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed for unknown format in 'final' schema.");
        }
        return false;
      }
    }

    // Perform validation on expanded form, first checking that the source data
    // item is a single entry map.
    else {
      if (sourceDataItem.getDataType() != UserDataType.NAMED_MAP) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed because source data is not a named map.");
        }
        return false;
      }
      final Map<String, DataItem<?>> sourceDataItemMap = sourceDataItem.castData();
      if (sourceDataItemMap.size() != 1) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed because source data is not a single entry map.");
        }
        return false;
      }

      // Extract the format identifier which determines the selection format
      // schema to be used for the data.
      for (final Entry<String, DataItem<?>> sourceDataItemEntry : sourceDataItemMap.entrySet()) {
        final String identName = sourceDataItemEntry.getKey();

        // Process known format identifiers.
        final SchemaNodeCore selectedDataSchema = formatIdentMap.get(identName);
        if (selectedDataSchema != null) {
          if (doRecursive) {
            return selectedDataSchema.validate(sourceDataItemEntry.getValue(), isTokenized, true, logger, loggerPath);
          }
        }

        // Fail for unknown format identifiers when using final schema
        // definitions.
        else if (schemaFinal) {
          if (logger != null) {
            logger.warning(loggerPath + " : Validation failed for unknown format in 'final' schema.");
          }
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

    // Extract the token which identifies the selection format schema to be
    // used for the data.
    final List<DataItem<?>> dataItemList = sourceDataItem.castData();
    final Long tokenValue = (Long) dataItemList.get(0).getData();

    // Process known token values
    final SchemaNodeCore selectedDataSchema = formatTokenMap.get(tokenValue);
    if (selectedDataSchema != null) {
      final String loggerFormatPath = (loggerPath == null) ? null : loggerPath + "." + selectedDataSchema.getName();
      final DataItem<?> expandedDataItem = selectedDataSchema.expand(dataItemList.get(1), logger, loggerFormatPath);
      if (expandedDataItem.getDecodeStatus().isFailure()) {
        return expandedDataItem;
      }
      final DataItem<Map<String, DataItem<?>>> expandedDataItemMap = dataItemFactory
          .createNamedMapItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.EXPANDED);
      expandedDataItemMap.getData().put(selectedDataSchema.getName(), expandedDataItem);
      return expandedDataItemMap;
    }

    // Substitute unknown format reference.
    else {
      final DataItem<?> expandedDataItem = dataItemFactory.createUndefinedItem(false, null)
          .setDecodeStatus(DecodeStatus.EXPANDED);
      final DataItem<Map<String, DataItem<?>>> expandedDataItemMap = dataItemFactory
          .createNamedMapItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.EXPANDED);
      expandedDataItemMap.getData().put("unknown", expandedDataItem);
      return expandedDataItemMap;
    }
  }

  /*
   * Implements SchemaNodeCore.tokenize(...)
   */
  @Override
  DataItem<?> tokenize(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {
    if (!validate(sourceDataItem, false, false, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    }

    // Extract the format identifier which determines the selection format
    // schema to be used for the data.
    final Map<String, DataItem<?>> sourceDataItemMap = sourceDataItem.castData();
    for (final Entry<String, DataItem<?>> sourceDataItemEntry : sourceDataItemMap.entrySet()) {
      final String identName = sourceDataItemEntry.getKey();

      // Process known format identifiers.
      final SchemaNodeCore selectedDataSchema = formatIdentMap.get(identName);
      if (selectedDataSchema != null) {
        final String loggerFormatPath = (loggerPath == null) ? null : loggerPath + "." + selectedDataSchema.getName();
        final DataItem<?> tokenizedDataItem = selectedDataSchema.tokenize(sourceDataItemEntry.getValue(), logger,
            loggerFormatPath);
        if (tokenizedDataItem.getDecodeStatus().isFailure()) {
          return tokenizedDataItem;
        }
        final DataItem<Long> tokenValueItem = dataItemFactory
            .createIntegerItem(selectedDataSchema.getTokenValue(), null).setDecodeStatus(DecodeStatus.TOKENIZED);
        final DataItem<List<DataItem<?>>> tokenizedDataItemList = dataItemFactory
            .createArrayItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.TOKENIZED);
        tokenizedDataItemList.getData().add(tokenValueItem);
        tokenizedDataItemList.getData().add(tokenizedDataItem);
        return tokenizedDataItemList;
      }

      // Substitute unknown format reference.
      else {
        final DataItem<?> tokenizedDataItem = dataItemFactory.createUndefinedItem(false, null)
            .setDecodeStatus(DecodeStatus.TOKENIZED);
        final DataItem<Long> tokenValueItem = dataItemFactory.createIntegerItem(Long.valueOf(0), null)
            .setDecodeStatus(DecodeStatus.TOKENIZED);
        final DataItem<List<DataItem<?>>> tokenizedDataItemList = dataItemFactory
            .createArrayItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.TOKENIZED);
        tokenizedDataItemList.getData().add(tokenValueItem);
        tokenizedDataItemList.getData().add(tokenizedDataItem);
        return tokenizedDataItemList;
      }
    }
    return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
  }
}
