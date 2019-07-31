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

/**
 * Defines the different types of schema node that are supported by the
 * tokenizing schema definitions.
 *
 * @author Chris Holgate
 */
enum SchemaDataType {

  /**
   * This primitive schema data type specifies that a boolean value is required.
   */
  BOOLEAN,

  /**
   * This primitive schema data type specifies that an integer value is required.
   */
  INTEGER,

  /**
   * This primitive schema data type specifies that a floating point value is
   * required. The floating point precision to be used is a property of the
   * floating point schema node.
   */
  NUMBER,

  /**
   * This primitive schema data type specifies that a UTF-8 encoded text string is
   * required.
   */
  TEXT_STRING,

  /**
   * This primitive schema data type specifies that a CBOR byte array or JSON
   * Base64 encoded byte string is required.
   */
  BYTE_STRING,

  /**
   * This schema data type specifies that an enumerated value is required.
   */
  ENUMERATED,

  /**
   * This composite schema data type specifies that an array of values is
   * required, where all the values match a common data entries schema.
   */
  ARRAY,

  /**
   * This composite schema data type specifies that a map is required, where the
   * map keys are arbitrary string values and the map values match a common data
   * entries schema.
   */
  MAP,

  /**
   * This composite schema data type specifies that a standard JSON/CBOR object is
   * required, where the member names and their required value data types are
   * specified by the schema. No tokenization is supported for standard objects.
   */
  STANDARD_OBJECT,

  /**
   * This composite schema data type specifies that a tokenizable JSON/CBOR object
   * is required, where the member names and their required value data types are
   * specified by the schema. The schema also specifies the mapping from member
   * names to token values for tokenization purposes.
   */
  TOKENIZABLE_OBJECT,

  /**
   * This composite schema data type specifies that a tokenizable JSON/CBOR
   * structure is required, where the member names and their required value data
   * types are specified by the schema. The schema also specifies the mapping from
   * member names to array positions for tokenization purposes.
   */
  STRUCTURE,

  /**
   * This composite schema data type specifies that a tokenizable JSON/CBOR
   * selection is required, where the option names and their required value data
   * types are specified by the schema. The schema also specifies the mapping of
   * option names to token values for tokenization purposes.
   */
  SELECTION

}
