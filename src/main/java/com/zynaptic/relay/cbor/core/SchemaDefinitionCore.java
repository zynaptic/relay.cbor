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

import java.util.logging.Logger;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.SchemaDefinition;

/**
 * Provides core implementation of the public schema definition API.
 *
 * @author Chris Holgate
 */
final class SchemaDefinitionCore implements SchemaDefinition {

  // Specifies the root of the schema hierarchical path to be used for reporting
  // validation warnings.
  private static final String LOGGER_PATH = "root";

  private final String title;
  private final SchemaNodeCore rootSchemaNode;
  private final Logger logger;

  /**
   * Provides protected default constructor.
   *
   * @param title This is the schema title that was extracted from the schema
   *   definition.
   * @param rootSchemaNode This is the root node of the schema validation tree.
   * @param logger This is the logger that is used to report schema validation
   *   issues.
   */
  SchemaDefinitionCore(final String title, final SchemaNodeCore rootSchemaNode, final Logger logger) {
    this.title = title;
    this.rootSchemaNode = rootSchemaNode;
    this.logger = logger;
  }

  /*
   * Implements SchemaDefinition.getTitle()
   */
  @Override
  public String getTitle() {
    return title;
  }

  /*
   * Implements SchemaDefinition.getLogger()
   */
  @Override
  public Logger getLogger() {
    return logger;
  }

  /*
   * Implements SchemaDefinition.createDefault(...)
   */
  @Override
  public DataItem<?> createDefault(final boolean includeAll) {
    return rootSchemaNode.createDefault(includeAll);
  }

  /*
   * Implements SchemaDefinition.validate(...)
   */
  @Override
  public boolean validate(final DataItem<?> sourceDataItem, final boolean isTokenized) {
    return rootSchemaNode.validate(sourceDataItem, isTokenized, true, logger, LOGGER_PATH);
  }

  /*
   * Implements SchemaDefinition.expand(...)
   */
  @Override
  public DataItem<?> expand(final DataItem<?> sourceDataItem) {
    return rootSchemaNode.expand(sourceDataItem, logger, LOGGER_PATH);
  }

  /*
   * Implements SchemaDefinition.tokenize(...)
   */
  @Override
  public DataItem<?> tokenize(final DataItem<?> sourceDataItem) {
    return rootSchemaNode.tokenize(sourceDataItem, logger, LOGGER_PATH);
  }
}
