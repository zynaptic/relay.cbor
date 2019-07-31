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
 * Extends generic CBOR data schema nodes for use with the boolean value data
 * type.
 *
 * @author Chris Holgate
 */
final class BooleanSchemaNode extends SchemaNodeCore {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  // Default boolean value for the schema node.
  private final boolean defaultValue;

  /**
   * Provides private default constructor which associates the schema node with a
   * given CBOR data item factory.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param defaultValue This is the default value associated with the boolean
   *   data schema node.
   */
  private BooleanSchemaNode(final DataItemFactory dataItemFactory, final boolean defaultValue) {
    this.dataItemFactory = dataItemFactory;
    this.defaultValue = defaultValue;
  }

  /**
   * Provides a static builder function which processes the supplied data item map
   * in order to generate a new boolean schema node.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema node.
   * @param schemaMap This is the attribute map which is to be processed in order
   *   to generate the schema node.
   * @param schemaPath This is the hierarchical path to the schema node to be used
   *   for error reporting.
   */
  static BooleanSchemaNode build(final DataItemFactory dataItemFactory, final Map<String, DataItem<?>> schemaMap,
      final String schemaPath) throws InvalidSchemaException {

    // Extract the default value, if present.
    boolean defaultValue;
    final DataItem<?> defaultItem = schemaMap.get("default");
    if (defaultItem == null) {
      defaultValue = false;
    } else {
      if (defaultItem.getDataType() != UserDataType.BOOLEAN) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'default' field not valid.");
      }
      defaultValue = (Boolean) defaultItem.getData();
    }

    // Build the boolean value schema.
    return new BooleanSchemaNode(dataItemFactory, defaultValue);
  }

  /*
   * Implements SchemaNodeCore.getSchemaDataType()
   */
  @Override
  SchemaDataType getSchemaDataType() {
    return SchemaDataType.BOOLEAN;
  }

  /*
   * Implements SchemaNodeCore.duplicate()
   */
  @Override
  BooleanSchemaNode duplicate() {
    final BooleanSchemaNode duplicatedSchema = new BooleanSchemaNode(dataItemFactory, defaultValue);
    duplicatedSchema.copyParamsFrom(this);
    return duplicatedSchema;
  }

  /*
   * Implements SchemaNodeCore.createDefault(...)
   */
  @Override
  DataItem<Boolean> createDefault(final boolean includeAll) {
    return dataItemFactory.createBooleanItem(defaultValue, getTagValues());
  }

  /*
   * Implements SchemaNodeCore.validate(...)
   */
  @Override
  boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized, final boolean doRecursive,
      final Logger logger, final String loggerPath) {
    if (sourceDataItem.getDataType() != UserDataType.BOOLEAN) {
      if (logger != null) {
        logger.warning(loggerPath + " : Validation failed because source data is not a boolean value.");
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
    if (!validate(sourceDataItem, true, false, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    } else {
      return sourceDataItem;
    }
  }

  /*
   * Implements SchemaNodeCore.tokenize(...)
   */
  @Override
  DataItem<?> tokenize(final DataItem<?> sourceDataItem, final Logger logger, final String loggerPath) {
    if (!validate(sourceDataItem, false, false, logger, loggerPath)) {
      return dataItemFactory.createInvalidItem(DecodeStatus.FAILED_SCHEMA);
    } else {
      return sourceDataItem;
    }
  }
}
