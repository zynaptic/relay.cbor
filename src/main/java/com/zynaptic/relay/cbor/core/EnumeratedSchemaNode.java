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
 * Extends generic CBOR data schema nodes for use with the enumerated value data
 * type.
 *
 * @author Chris Holgate
 */
final class EnumeratedSchemaNode extends SchemaNodeCore {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  // Expansion map from integer representations to enumeration strings.
  private final Map<Long, String> expansionMap;

  // Tokenizing map from enumeration strings to integer representations.
  private final Map<String, Long> tokenizingMap;

  // Specifies whether the defined schema should be treated as final.
  private final boolean schemaFinal;

  // Specifies the default value for the enumeration.
  private final String defaultValue;

  /**
   * Provides private default constructor which associates the schema node with a
   * given CBOR data item factory.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param expansionMap This is the mapping to be used for expanding the
   *   tokenized enumeration values to their string representations.
   * @param tokenizingMap This is the mapping to be used for tokenizing the
   *   enumeration values from their string representations.
   * @param schemaFinal Specifies whether the defined schema should be treated as
   *   final.
   * @param defaultValue Specifies the default enumerated value associated with
   *   this enumeration.
   */
  private EnumeratedSchemaNode(final DataItemFactory dataItemFactory, final Map<Long, String> expansionMap,
      final Map<String, Long> tokenizingMap, final boolean schemaFinal, final String defaultValue) {
    this.dataItemFactory = dataItemFactory;
    this.expansionMap = expansionMap;
    this.tokenizingMap = tokenizingMap;
    this.schemaFinal = schemaFinal;
    this.defaultValue = defaultValue;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new enumerated schema node.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param schemaMap This is the attribute map which is to be processed in order
   *   to generate the schema node.
   * @param schemaPath This is the hierarchical path to the schema node to be used
   *   for error reporting.
   */
  static EnumeratedSchemaNode build(final DataItemFactory dataItemFactory, final Map<String, DataItem<?>> schemaMap,
      final String schemaPath) throws InvalidSchemaException {

    // Determine if the enumeration is extensible or final.
    boolean schemaFinal;
    final DataItem<?> finalFlagItem = schemaMap.get("final");
    if (finalFlagItem == null) {
      schemaFinal = false;
    } else if (finalFlagItem.getDataType() == UserDataType.BOOLEAN) {
      schemaFinal = (Boolean) finalFlagItem.getData();
    } else {
      throw new InvalidSchemaException(schemaPath + " : CBOR enumerated schema 'final' field not valid.");
    }

    // Extract the map of string representations to numeric values.
    final DataItem<?> enumValuesItem = schemaMap.get("values");
    if ((enumValuesItem == null) || (enumValuesItem.getDataType() != UserDataType.NAMED_MAP)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR enumerated schema 'values' field not valid.");
    }
    final Map<String, DataItem<?>> enumValuesItemMap = enumValuesItem.castData();

    // Create two hash maps for implementing the forward and reverse lookups.
    final Map<String, Long> tokenizingMap = new HashMap<String, Long>(enumValuesItemMap.size());
    final Map<Long, String> expansionMap = new HashMap<Long, String>(enumValuesItemMap.size());

    // Process all of the enumerated values in the schema.
    for (final Map.Entry<String, DataItem<?>> enumValuesEntry : enumValuesItemMap.entrySet()) {
      if ((enumValuesEntry.getValue() == null) || (enumValuesEntry.getValue().getDataType() != UserDataType.INTEGER)) {
        throw new InvalidSchemaException(
            schemaPath + ".values" + " : CBOR enumeration not valid for '" + enumValuesEntry.getKey() + "'.");
      }
      final Long enumToken = (Long) enumValuesEntry.getValue().getData();

      // Check for reserved enumeration string.
      if (enumValuesEntry.getKey().equals("unknown")) {
        throw new InvalidSchemaException(schemaPath + ".values" + " : CBOR enumeration string 'unknown' is reserved.");
      }

      // Check for duplicate or reserved token values.
      if (enumToken == 0) {
        throw new InvalidSchemaException(
            schemaPath + ".values" + " : CBOR enumeration token value 0 is reserved for 'unknown'.");
      }
      if (expansionMap.containsKey(enumToken)) {
        throw new InvalidSchemaException(
            schemaPath + ".values" + " : CBOR enumeration has duplicate token for '" + enumValuesEntry.getKey() + "'.");
      }
      expansionMap.put(enumToken, enumValuesEntry.getKey());
      tokenizingMap.put(enumValuesEntry.getKey(), enumToken);
    }

    // Extract the default enumeration string, checking that it is within the
    // defined set.
    final DataItem<?> defaultItem = schemaMap.get("default");
    if ((defaultItem == null) || (defaultItem.getDataType() != UserDataType.TEXT_STRING)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR enumerated schema 'default' field not valid.");
    }
    final String defaultValue = (String) defaultItem.getData();
    if (!tokenizingMap.containsKey(defaultValue)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR enumerated schema 'default' field not in enumeration.");
    }

    // Build the enumerated data schema.
    return new EnumeratedSchemaNode(dataItemFactory, expansionMap, tokenizingMap, schemaFinal, defaultValue);
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.ENUMERATED;
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  EnumeratedSchemaNode duplicate() {
    final EnumeratedSchemaNode duplicatedSchema = new EnumeratedSchemaNode(dataItemFactory, expansionMap, tokenizingMap,
        schemaFinal, defaultValue);
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

    // Perform validation on tokenized form.
    if (isTokenized) {
      if (sourceDataItem.getDataType() != UserDataType.INTEGER) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed because source data is not an integer token.");
        }
        return false;
      }

      // Fail on unknown token values if the enumeration is specified as being
      // 'final'.
      if (schemaFinal) {
        if (!expansionMap.containsKey(sourceDataItem.getData())) {
          if (logger != null) {
            logger.warning(loggerPath + " : Validation failed because unknown token specified for 'final' schema.");
          }
          return false;
        }
      }
    }

    // Perform validation on expanded form.
    else {
      if (sourceDataItem.getDataType() != UserDataType.TEXT_STRING) {
        if (logger != null) {
          logger.warning(loggerPath + " : Validation failed because source data is not an enumeration string.");
        }
        return false;
      }

      // Fail on unknown enumeration strings if the enumeration is specified as
      // being 'final'.
      if (schemaFinal) {
        if (!tokenizingMap.containsKey(sourceDataItem.getData())) {
          if (logger != null) {
            logger
                .warning(loggerPath + " : Validation failed because unknown enumeration specified for 'final' schema.");
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

    // Attempt to extract the string representation of the enumerated value. For
    // non-final enumerations, unknown token values are substituted with the
    // 'unknown' string.
    final String enumString = expansionMap.get(sourceDataItem.getData());
    if (enumString == null) {
      return dataItemFactory.createTextStringItem("unknown", sourceDataItem.getTags())
          .setDecodeStatus(DecodeStatus.EXPANDED);
    } else {
      return dataItemFactory.createTextStringItem(enumString, sourceDataItem.getTags())
          .setDecodeStatus(DecodeStatus.EXPANDED);
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

    // Attempt to encode the string representation of the enumerated value. For
    // non-final enumerations, unknown enumeration strings are substituted with
    // the unknown token.
    final Long tokenValue = tokenizingMap.get(sourceDataItem.getData());
    if (tokenValue == null) {
      return dataItemFactory.createIntegerItem(0, sourceDataItem.getTags()).setDecodeStatus(DecodeStatus.TOKENIZED);
    } else {
      return dataItemFactory.createIntegerItem(tokenValue, sourceDataItem.getTags())
          .setDecodeStatus(DecodeStatus.TOKENIZED);
    }
  }
}
