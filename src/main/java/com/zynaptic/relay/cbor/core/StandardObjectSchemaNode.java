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
import java.util.Map;
import java.util.logging.Logger;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DataItemFactory;
import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.InvalidSchemaException;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Extends generic CBOR data schema nodes for use with standard non-tokenizable
 * object data types.
 *
 * @author Chris Holgate
 */
final class StandardObjectSchemaNode extends SchemaNodeCore {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  // Unordered list of schema properties for the object.
  private final SchemaNodeCore[] schemaProperties;

  // Specifies whether the defined schema should be treated as final.
  private final boolean schemaFinal;

  /**
   * Provides private default constructor which associates the schema node with a
   * given CBOR data item factory.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param schemaProperties This is the set of schema properties which are
   *   encapsulated by the CBOR object schema.
   * @param schemaFinal Specifies whether the defined schema should be treated as
   *   final.
   */
  private StandardObjectSchemaNode(final DataItemFactory dataItemFactory, final SchemaNodeCore[] schemaProperties,
      final boolean schemaFinal) {
    this.dataItemFactory = dataItemFactory;
    this.schemaProperties = schemaProperties;
    this.schemaFinal = schemaFinal;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new object schema node.
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
  static StandardObjectSchemaNode build(final DataItemFactory dataItemFactory, final SchemaBuilder schemaBuilder,
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
      throw new InvalidSchemaException(schemaPath + " : CBOR object schema 'final' field not valid.");
    }

    // Extract the set of object property definitions.
    final DataItem<?> propertyMapItem = schemaMap.get("properties");
    if ((propertyMapItem == null) || (propertyMapItem.getDataType() != UserDataType.NAMED_MAP)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR object schema 'properties' field not valid.");
    }
    final Map<String, DataItem<?>> sourcePropertyMap = propertyMapItem.castData();

    // Create an array for storing the properties in arbitrary order.
    final SchemaNodeCore[] schemaProperties = new SchemaNodeCore[sourcePropertyMap.size()];
    final Map<String, SchemaNodeCore> schemaPropertyMap = new HashMap<String, SchemaNodeCore>(sourcePropertyMap.size());

    // Extract all object properties in turn.
    int index = 0;
    for (final Map.Entry<String, DataItem<?>> sourceMapEntry : sourcePropertyMap.entrySet()) {
      final String propertyName = sourceMapEntry.getKey();
      final DataItem<?> propertyItem = sourceMapEntry.getValue();
      if ((propertyItem == null) || (propertyItem.getDataType() != UserDataType.NAMED_MAP)) {
        throw new InvalidSchemaException(
            schemaPath + " : CBOR object schema property '" + propertyName + "' is not valid.");
      }
      final Map<String, DataItem<?>> propertyItemMap = propertyItem.castData();

      // Determine if the property required flag is present and set to a
      // legitimate boolean value.
      boolean requiredFlag = false;
      final DataItem<?> requiredItem = propertyItemMap.get("required");
      if (requiredItem != null) {
        if (requiredItem.getDataType() != UserDataType.BOOLEAN) {
          throw new InvalidSchemaException(
              schemaPath + " : CBOR object schema property '" + propertyName + "' has invalid 'required' field.");
        }
        requiredFlag = ((Boolean) requiredItem.getData()).booleanValue();
      }

      // Perform recursive processing on the property item to generate the
      // corresponding schema node and then insert it into the unordered array.
      final SchemaNodeCore propertySchema = schemaBuilder.recursiveBuild(propertyItemMap, schemaDefinitions,
          schemaPath + ".properties." + propertyName);
      propertySchema.setName(propertyName);
      propertySchema.setIsOptional(!requiredFlag);
      schemaProperties[index++] = propertySchema;
      schemaPropertyMap.put(propertyName, propertySchema);
    }

    // Build the object data schema.
    return new StandardObjectSchemaNode(dataItemFactory, schemaProperties, schemaFinal);
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.STANDARD_OBJECT;
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  StandardObjectSchemaNode duplicate() {
    final StandardObjectSchemaNode duplicatedSchema = new StandardObjectSchemaNode(dataItemFactory, schemaProperties,
        schemaFinal);
    duplicatedSchema.copyParamsFrom(this);
    return duplicatedSchema;
  }

  /*
   * Implements SchemaNodeCore.createDefault(...)
   */
  @Override
  DataItem<Map<String, DataItem<?>>> createDefault(final boolean includeAll) {
    final DataItem<Map<String, DataItem<?>>> objectDataItem = dataItemFactory.createNamedMapItem(getTagValues(), true);
    for (final SchemaNodeCore schemaProperty : schemaProperties) {
      if ((includeAll) || (!schemaProperty.isOptional())) {
        final DataItem<?> propertyDataItem = schemaProperty.createDefault(includeAll);
        objectDataItem.getData().put(schemaProperty.getName(), propertyDataItem);
      }
    }
    return objectDataItem;
  }

  /*
   * Implements SchemaNodeCore.validate(...)
   */
  @Override
  boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized, final boolean doRecursive,
      final Logger logger, final String loggerPath) {

    // Check that the source data item is a conventional named map.
    if (sourceDataItem.getDataType() != UserDataType.NAMED_MAP) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because source data is not a named map.");
      }
      return false;
    }

    // Perform validation of the map entries.
    int propertyCount = 0;
    final Map<String, DataItem<?>> propertyDataMap = sourceDataItem.castData();
    for (final SchemaNodeCore schemaProperty : schemaProperties) {
      final String propertyName = schemaProperty.getName();
      final DataItem<?> propertyDataItem = propertyDataMap.get(propertyName);

      // Optional entries can be absent from the map.
      if (propertyDataItem == null) {
        if (!schemaProperty.isOptional()) {
          if (logger != null) {
            logger.warning(
                loggerPath + " : Validation failed because required property '" + propertyName + "' is not present.");
          }
          return false;
        }
      }

      // Entries are only validated if recursive validation has been selected.
      else {
        if (doRecursive) {
          final String loggerPropertyPath = (loggerPath == null) ? null : loggerPath + "." + propertyName;
          if (!schemaProperty.validate(propertyDataItem, isTokenized, true, logger, loggerPropertyPath)) {
            return false;
          }
        }
        propertyCount += 1;
      }
    }

    // Check that all records have been processed for 'final' structures.
    if (schemaFinal && (propertyCount != propertyDataMap.size())) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed on unknown property for 'final' object schema.");
      }
      return false;
    } else {
      return true;
    }
  }

  /*
   * Implements SchemaNodeCore.expand(...)
   */
  @Override
  DataItem<?> expand(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {
    if (!validate(sourceDataItem, true, false, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    }

    // Perform expansion on all the source data properties.
    final Map<String, DataItem<?>> propertyDataMap = sourceDataItem.castData();
    final DataItem<Map<String, DataItem<?>>> expandedDataItemMap = dataItemFactory
        .createNamedMapItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.EXPANDED);
    for (final SchemaNodeCore schemaProperty : schemaProperties) {
      final String propertyName = schemaProperty.getName();
      final DataItem<?> originalDataItem = propertyDataMap.get(propertyName);

      // Expand the property data prior to adding it to the data item map.
      if (originalDataItem != null) {
        final String loggerPropertyPath = (loggerPath == null) ? null : loggerPath + "." + propertyName;
        final DataItem<?> expandedDataItem = schemaProperty.expand(originalDataItem, logger, loggerPropertyPath);
        if (expandedDataItem.getDecodeStatus().isFailure()) {
          return expandedDataItem;
        }
        expandedDataItemMap.getData().put(schemaProperty.getName(), expandedDataItem);
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

    // Perform expansion on all the source data properties.
    final Map<String, DataItem<?>> propertyDataMap = sourceDataItem.castData();
    final DataItem<Map<String, DataItem<?>>> tokenizedDataItemMap = dataItemFactory
        .createNamedMapItem(sourceDataItem.getTags(), false).setDecodeStatus(DecodeStatus.TOKENIZED);
    for (final SchemaNodeCore schemaProperty : schemaProperties) {
      final String propertyName = schemaProperty.getName();
      final DataItem<?> originalDataItem = propertyDataMap.get(propertyName);

      // Tokenize the property data prior to adding it to the data item map.
      if (originalDataItem != null) {
        final String loggerPropertyPath = (loggerPath == null) ? null : loggerPath + "." + propertyName;
        final DataItem<?> tokenizedDataItem = schemaProperty.tokenize(originalDataItem, logger, loggerPropertyPath);
        if (tokenizedDataItem.getDecodeStatus().isFailure()) {
          return tokenizedDataItem;
        }
        tokenizedDataItemMap.getData().put(schemaProperty.getName(), tokenizedDataItem);
      }
    }
    return tokenizedDataItemMap;
  }
}
