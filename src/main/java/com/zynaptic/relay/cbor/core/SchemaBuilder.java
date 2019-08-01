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
import com.zynaptic.relay.cbor.InvalidSchemaException;
import com.zynaptic.relay.cbor.SchemaDefinition;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides the ability to build schema processing trees from a supplied set of
 * CBOR data items.
 *
 * @author Chris Holgate
 */
final class SchemaBuilder {

  // Local reference to the CBOR data item factory.
  private final DataItemFactory dataItemFactory;

  /**
   * Provides default constructor which associates the schema builder with a given
   * CBOR data item factory.
   *
   * @param dataItemFactory This is the CBOR data item factory which is to be used
   *   by the schema builder.
   */
  public SchemaBuilder(final DataItemFactory dataItemFactory) {
    this.dataItemFactory = dataItemFactory;
  }

  /**
   * Builds a schema processing tree from the supplied set of CBOR data items.
   *
   * @param schemaDataItem This is the CBOR data item which represents the root
   *   node in a CBOR schema definition.
   * @param logger This is a standard Java logging interface which may be used to
   *   report details of validation, expansion and tokenization failures for the
   *   specified schema. A null reference may be specified if no failure logging
   *   is to be carried out.
   * @return Returns a schema root node object which corresponds to the root of
   *   the generated schema processing tree.
   * @throws InvalidSchemaException This exception will be thrown if the supplied
   *   schema data item does not encode a valid schema description.
   */
  SchemaDefinition build(final DataItem<Map<String, DataItem<?>>> schemaDataItem, final Logger logger)
      throws InvalidSchemaException {

    // Extract the root schema map which contains the mapping between root
    // schema object properties and their associated values.
    if (schemaDataItem.getDataType() != UserDataType.NAMED_MAP) {
      throw new InvalidSchemaException("CBOR schema toplevel node must be a named map.");
    }

    // Process root schema terms, starting with the title field that is used as
    // the schema name.
    final Map<String, DataItem<?>> schemaMap = schemaDataItem.getData();
    final DataItem<?> schemaTitleItem = schemaMap.get("title");
    if ((schemaTitleItem == null) || (schemaTitleItem.getDataType() != UserDataType.TEXT_STRING)) {
      throw new InvalidSchemaException("CBOR schema toplevel node 'title' field not present.");
    }
    final String title = (String) schemaTitleItem.getData();

    // TODO: Remaining root node fields.

    // Perform recursive processing on common schema type definitions in the
    // 'definitions' section. This is an optional section.
    Map<String, SchemaNodeCore> schemaDefinitions = null;
    final DataItem<?> schemaDefinitionsItem = schemaMap.get("definitions");
    if (schemaDefinitionsItem != null) {
      if (schemaDefinitionsItem.getDataType() != UserDataType.NAMED_MAP) {
        throw new InvalidSchemaException("CBOR schema toplevel node 'definitions' field not valid.");
      }
      final Map<String, DataItem<?>> schemaDefinitionsMap = schemaDefinitionsItem.castData();
      schemaDefinitions = new HashMap<String, SchemaNodeCore>(schemaDefinitionsMap.size());

      // Extract schema prototype definitions for all map entries.
      for (final Map.Entry<String, DataItem<?>> definitionEntry : schemaDefinitionsMap.entrySet()) {
        final String definitionName = definitionEntry.getKey();
        final SchemaNodeCore definitionSchema = recursiveBuild(definitionEntry.getValue(), null,
            "definitions." + definitionName);
        schemaDefinitions.put(definitionName, definitionSchema);
      }
    }

    // Perform recursive processing on standard schema terms in the root
    // node, supplying the generated schema definitions.
    final DataItem<?> schemaRootItem = schemaMap.get("root");
    final SchemaNodeCore dataSchema = recursiveBuild(schemaRootItem, schemaDefinitions, "root");
    return new SchemaDefinitionCore(title, dataSchema, logger);
  }

  /**
   * Perform recursive processing on the schema node, given a map of schema field
   * names to CBOR data items.
   *
   * @param schemaMap This is the attribute map which is to be processed in order
   *   to generate the schema node.
   * @param schemaDefinitions This is the set of pre-specified schema definitions
   *   which may be used in place of conventional data types.
   * @param schemaPath This is the hierarchical path to the schema node to be used
   *   for error reporting.
   */
  SchemaNodeCore recursiveBuild(final Map<String, DataItem<?>> schemaMap,
      final Map<String, SchemaNodeCore> schemaDefinitions, final String schemaPath) throws InvalidSchemaException {
  
    // Extract the data type field from the schema map.
    final DataItem<?> schemaTypeItem = schemaMap.get("type");
    if ((schemaTypeItem == null) || (schemaTypeItem.getDataType() != UserDataType.TEXT_STRING)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR schema 'type' field not valid.");
    }
  
    // Build the appropriate schema entry for the specified data type.
    SchemaNodeCore dataSchema;
    final String schemaTypeName = (String) schemaTypeItem.getData();
    switch (schemaTypeName) {
    case "object":
      dataSchema = buildObjectSchema(schemaMap, schemaDefinitions, schemaPath);
      break;
    case "structure":
      dataSchema = StructureSchemaNode.build(dataItemFactory, this, schemaMap, schemaDefinitions, schemaPath);
      break;
    case "map":
      dataSchema = MapSchemaNode.build(dataItemFactory, this, schemaMap, schemaDefinitions, schemaPath);
      break;
    case "array":
      dataSchema = ArraySchemaNode.build(dataItemFactory, this, schemaMap, schemaDefinitions, schemaPath);
      break;
    case "string":
      dataSchema = TextStringSchemaNode.build(dataItemFactory, schemaMap, schemaPath);
      break;
    case "encoded":
      dataSchema = ByteStringSchemaNode.build(dataItemFactory, schemaMap, schemaPath);
      break;
    case "enumerated":
      dataSchema = EnumeratedSchemaNode.build(dataItemFactory, schemaMap, schemaPath);
      break;
    case "integer":
      dataSchema = IntegerSchemaNode.build(dataItemFactory, schemaMap, schemaPath);
      break;
    case "number":
      dataSchema = NumberSchemaNode.build(dataItemFactory, schemaMap, schemaPath);
      break;
    case "boolean":
      dataSchema = BooleanSchemaNode.build(dataItemFactory, schemaMap, schemaPath);
      break;
    case "selection":
      dataSchema = SelectionSchemaNode.build(dataItemFactory, this, schemaMap, schemaDefinitions, schemaPath);
      break;
  
    // User defined data types are extracted from the existing list of
    // pre-declared schema type definitions. Allows common options to be
    // overridden for a given instance.
    default:
      final SchemaNodeCore definedSchema = (schemaDefinitions == null) ? null : schemaDefinitions.get(schemaTypeName);
      if (definedSchema == null) {
        throw new InvalidSchemaException(
            schemaPath + " : CBOR schema 'type' field has unknown value '" + schemaTypeName + "'.");
      }
      dataSchema = definedSchema.duplicate();
      break;
    }
    dataSchema.setTypeName(schemaTypeName);
    processCommonOptions(schemaMap, schemaPath, dataSchema);
    return dataSchema;
  }

  /*
   * Extracts the schema map which contains the mapping between schema object
   * properties and their associated values and then processes the schema node.
   */
  private SchemaNodeCore recursiveBuild(final DataItem<?> schemaDataItem,
      final Map<String, SchemaNodeCore> schemaDefinitions, final String schemaPath) throws InvalidSchemaException {
    if ((schemaDataItem == null) || (schemaDataItem.getDataType() != UserDataType.NAMED_MAP)) {
      throw new InvalidSchemaException(schemaPath + " : CBOR schema node must be a JSON object.");
    }
    final Map<String, DataItem<?>> schemaMap = schemaDataItem.castData();
    return recursiveBuild(schemaMap, schemaDefinitions, schemaPath);
  }

  /*
   * Builds an object schema, with the underlying schema type being selected using
   * the 'tokenize' flag.
   */
  private SchemaNodeCore buildObjectSchema(final Map<String, DataItem<?>> schemaMap,
      final Map<String, SchemaNodeCore> schemaDefinitions, final String schemaPath) throws InvalidSchemaException {

    // Check the tokenizable flag to determine the type of schema to
    // instantiate.
    boolean tokenizable;
    final DataItem<?> tokenizationFlagItem = schemaMap.get("tokenize");
    if (tokenizationFlagItem == null) {
      tokenizable = false;
    } else if (tokenizationFlagItem.getDataType() == UserDataType.BOOLEAN) {
      tokenizable = (Boolean) tokenizationFlagItem.getData();
    } else {
      throw new InvalidSchemaException(schemaPath + " : CBOR object schema 'tokenize' field not valid.");
    }

    // Instantiate either the standard object schema or the tokenizable object
    // schema.
    if (tokenizable) {
      return TokenizableObjectSchemaNode.build(dataItemFactory, this, schemaMap, schemaDefinitions, schemaPath);
    } else {
      return StandardObjectSchemaNode.build(dataItemFactory, this, schemaMap, schemaDefinitions, schemaPath);
    }
  }

  /*
   * Process common options for all schema nodes.
   */
  private void processCommonOptions(final Map<String, DataItem<?>> schemaMap, final String schemaPath,
      final SchemaNodeCore dataSchema) throws InvalidSchemaException {

    // Extract the common schema description field, if present.
    final DataItem<?> descriptionItem = schemaMap.get("description");
    if (descriptionItem != null) {
      if (descriptionItem.getDataType() != UserDataType.TEXT_STRING) {
        throw new InvalidSchemaException(schemaPath + " : CBOR schema 'description' field not valid.");
      }
      dataSchema.setDescription((String) descriptionItem.getData());
    }
  }
}
