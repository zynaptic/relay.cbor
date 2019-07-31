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

/**
 * Provides common functionality for CBOR schema item nodes. This includes
 * support for storing common schema attribute values.
 *
 * @author Chris Holgate
 */
abstract class SchemaNodeCore {

  // Specify the maximum supported integer value, taking into account JavaScript
  // limitations.
  static final long MAX_INTEGER_VALUE = 1L << 53;

  // Specify the minimum supported integer value, taking into account JavaScript
  // limitations.
  static final long MIN_INTEGER_VALUE = -1L << 53;

  // Specify limit values for half precision floating point representation.
  static final int MAX_HALF_PRECISION_FLOAT = 65504;

  // Stores the data schema name, if present.
  private String name = null;

  // Stores the data schema description, if present.
  private String description = null;

  // Stores the data schema type, if present.
  private String typeName = null;

  // Stores the CBOR tag values as an integer array, if present.
  private int[] tagValues;

  // Stores the data schema token or index value, if present.
  private Long tokenValue = null;

  // Stores the data schema optional flag value.
  private boolean optional = true;

  /**
   * Creates a duplicate copy of the data schema for inclusion in the graph.
   *
   * @return Returns a duplicate copy of the data schema.
   */
  abstract SchemaNodeCore duplicate();

  /**
   * Creates a new data item which conforms to the schema, using the default
   * values specified for the schema. The data item is generated in its expanded
   * form and may subsequently be modified in order to assign non-default values.
   *
   * @param includeAll If set to 'false' only those attributes or records which
   *   are specified as being 'required' in the schema are generated. If set to
   *   'true', all attributes and records are generated.
   * @return Returns a newly generated default data item for the schema.
   */
  abstract DataItem<?> createDefault(boolean includeAll);

  /**
   * Performs validation on the specified source data item using the local schema
   * node rules.
   *
   * @param sourceDataItem This is the source data item for which validation is to
   *   be carried out.
   * @param isTokenized This is a boolean flag which should be set to 'true' if
   *   the source data item is in its tokenized form.
   * @param doRecursive This is a boolean flag which when set to 'true' implements
   *   recursive validation. If set to 'false' validation is only carried out for
   *   the current level in the schema hierarchy.
   * @param logger This is a standard Java logger object which may be used to
   *   report schema validation warnings. A null reference may be supplied if no
   *   logging is to be carried out.
   * @param loggerPath This is the path through the data hierarchy which will be
   *   used for reporting schema validation warnings.
   * @return Returns a boolean value which will be set to 'true' if the source
   *   data item has been validated against the schema definition and 'false'
   *   otherwise.
   */
  abstract boolean validate(DataItem<?> sourceDataItem, boolean isTokenized, boolean doRecursive, Logger logger,
      String loggerPath);

  /**
   * Performs token expansion on the specified source data item using the local
   * schema node rules.
   *
   * @param sourceDataItem This is the source data item for which token expansion
   *   is to be carried out.
   * @param logger This is a standard Java logger object which may be used to
   *   report schema validation warnings. A null reference may be supplied if no
   *   logging is to be carried out.
   * @param loggerPath This is the path through the data hierarchy which will be
   *   used for reporting schema validation warnings.
   * @return Returns a data item which corresponds to the original source data
   *   after token expansion has been completed. If the source data did not match
   *   the schema node rules this data item will be returned with a status of
   *   {@link com.zynaptic.cbor.DecodeStatus#FAILED_SCHEMA}.
   */
  abstract DataItem<?> expand(DataItem<?> sourceDataItem, Logger logger, String loggerPath);

  /**
   * Performs tokenization on the specified source data using the local schema
   * node rules.
   *
   * @param sourceDataItem This is the source data item for which tokenization is
   *   to be carried out.
   * @param logger This is a standard Java logger object which may be used to
   *   report schema validation warnings. A null reference may be supplied if no
   *   logging is to be carried out.
   * @param loggerPath This is the path through the data hierarchy which will be
   *   used for reporting schema validation warnings.
   * @return Returns a data item which corresponds to the original source data
   *   after tokenization has been completed. If the source data did not match the
   *   schema node rules this data item will be returned with a status of
   *   {@link com.zynaptic.cbor.DecodeStatus#FAILED_SCHEMA}.
   */
  abstract DataItem<?> tokenize(DataItem<?> sourceDataItem, Logger logger, String loggerPath);

  /**
   * Accesses the schema data type associated with the schema node. This is used
   * to determine which JSON/CBOR data types are consistent with the schema.
   *
   * @return Returns the schema data type associated with the schema node.
   */
  abstract SchemaDataType getSchemaDataType();

  /**
   * Sets the name of this schema node which is used by an enclosing parent schema
   * to identify it. This corresponds to the label in the JSON based "records" map
   * for structure definitions and the label in the equivalent "properties" map
   * for JSON object definitions.
   *
   * @param name This is the name by which the parent schema identifies this
   *   schema node.
   */
  final void setName(final String name) {
    this.name = name;
  }

  /**
   * Sets the human-readable description field value for the schema definition.
   * This corresponds to the value associated with the "description" label in the
   * JSON based schema definition.
   *
   * @param description This is the human-readable description field which is
   *   included in the schema definition.
   */
  final void setDescription(final String description) {
    this.description = description;
  }

  /**
   * Sets the schema data type string which is associated with the schema
   * definition. This corresponds to the value associated with the "type" label in
   * the JSON based schema definition.
   *
   * @param typeName This is a string value which indicates the schema data type.
   */
  final void setTypeName(final String typeName) {
    this.typeName = typeName;
  }

  /**
   * Sets the CBOR tag value array which is associated with the schema definition.
   *
   * @param tagValues This is an array of CBOR tag values which is associated with
   *   the schema definition.
   */
  final void setTagValues(final int[] tagValues) {
    this.tagValues = (tagValues == null) ? null : tagValues.clone();
  }

  /**
   * Sets the token value which is associated with the schema definition. This is
   * the integer encoding which is used as a substitute for the schema name in the
   * tokenized representation. This corresponds to the value associated with the
   * "index" label in JSON based structure definitions or the "token" label in
   * tokenized maps.
   *
   * @param tokenValue This is the integer value which is used to encode the
   *   schema name in the tokenized representation.
   */
  final void setTokenValue(final Long tokenValue) {
    this.tokenValue = tokenValue;
  }

  /**
   * Sets the boolean flag which is used to indicate whether or not a schema node
   * is optional. This corresponds to the value associated with the "optional"
   * label in the JSON based schema definition.
   *
   * @param optional This is a boolean value which will be set to 'true' if the
   *   schema node relates to an optional data item.
   */
  final void setIsOptional(final boolean optional) {
    this.optional = optional;
  }

  /**
   * Sets the schema parameters to match those of the source schema.
   *
   * @param sourceSchema This is the source schema from which the schema
   *   parameters are to be derived.
   */
  final void copyParamsFrom(final SchemaNodeCore sourceSchema) {
    name = sourceSchema.name;
    description = sourceSchema.description;
    typeName = sourceSchema.typeName;
    tokenValue = sourceSchema.tokenValue;
    optional = sourceSchema.optional;
    tagValues = sourceSchema.getTagValues();
  }

  /**
   * Accesses the name of the schema definition item, as extracted from the schema
   * definition.
   *
   * @return Returns the name of the schema definition item.
   */
  final String getName() {
    return name;
  }

  /**
   * Accesses the description string of the schema definition item, as extracted
   * from the schema definition.
   *
   * @return Returns the description of the schema definition item.
   */
  final String getDescription() {
    return description;
  }

  /**
   * Accesses the type name associated with the schema definition item, as
   * extracted from the schema definition.
   *
   * @return Returns the type name associated with the schema definition item.
   */
  final String getTypeName() {
    return typeName;
  }

  /**
   * Accesses the tag values associated with the schema definition item.
   *
   * @return Returns the tag values associated with the schema definition item.
   */
  final int[] getTagValues() {
    return (tagValues == null) ? null : tagValues.clone();
  }

  /**
   * Gets the token value which is associated with the schema definition.
   *
   * @return Returns an integer value which is used to encode the schema name in
   *   the tokenized representation.
   */
  final Long getTokenValue() {
    return tokenValue;
  }

  /**
   * Gets the optional data flag which is associated with the schema definition.
   *
   * @return Returns a boolean flag which will be set to 'true' if the schema node
   *   relates to an optional data item.
   */
  final boolean isOptional() {
    return optional;
  }
}
