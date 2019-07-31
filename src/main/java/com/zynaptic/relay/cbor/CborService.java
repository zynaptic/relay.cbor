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

package com.zynaptic.relay.cbor;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Provides an interface to the CBOR service factory, which may be used for
 * accessing the various JSON and CBOR service interfaces provided by the
 * module.
 *
 * @author Chris Holgate
 */
public interface CborService {

  /**
   * Accesses the CBOR data item factory, which may be used for programmatically
   * generating JSON and CBOR messages.
   *
   * @return Returns a reference to the CBOR data item factory to be used for
   *   directly manipulating JSON and CBOR messages.
   */
  public DataItemFactory getDataItemFactory();

  /**
   * Accesses the CBOR data streamer, which may be used for writing a CBOR data
   * item to CBOR or JSON output streams or reading a CBOR data item from CBOR or
   * JSON input streams.
   *
   * @return Returns a reference to the CBOR data streamer to be used for encoding
   *   and decoding CBOR data items from CBOR and JSON formatted streams.
   */
  public DataStreamer getDataStreamer();

  /**
   * Builds a new schema definition object, given a data item which encodes the
   * toplevel schema definition as a named map. A standard Java logger component
   * may be specified, which will be used to log warnings that are generated
   * during data validation, tokenisation and expansion.
   *
   * @param schemaDataItem This is the CBOR data item which is to be used to build
   *   the schema definition. Note that it must have a user type of
   *   {@link UserDataType#NAMED_MAP}, which dictates the generic type definition
   *   specified here. An unchecked cast may be used in order to convert a
   *   wildcard data item to one with the required generic type, since runtime
   *   checks will throw an {@link InvalidSchemaException} if the data item has
   *   the incorrect user type.
   * @param logger This is a standard Java logger which will be used to log
   *   warning messages that are generated when a supplied data item fails to
   *   validate against the schema definition. A null reference may be passed if
   *   no logger is to be used.
   * @return Returns a newly generated schema definition object which implements
   *   the supplied schema definition.
   * @throws InvalidSchemaException This exception will be thrown if the supplied
   *   schema data item does not conform to the schema definition syntax.
   */
  public SchemaDefinition getSchemaDefinition(DataItem<Map<String, DataItem<?>>> schemaDataItem, Logger logger)
      throws InvalidSchemaException;

}
