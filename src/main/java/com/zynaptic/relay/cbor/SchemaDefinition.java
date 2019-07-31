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

import java.util.logging.Logger;

/**
 * Provides the public interface for accessing CBOR schema definition objects.
 *
 * @author Chris Holgate
 */
public interface SchemaDefinition {

  /**
   * Accesses the schema title, as defined in the original schema definition using
   * the 'title' attribute.
   *
   * @return Returns the title string which is associated with the schema
   *   definition.
   */
  public String getTitle();

  /**
   * Accesses the standard Java logger object which is used for reporting
   * validation, tokenization and expansion warning messages associated with the
   * schema definition.
   *
   * @return Returns the standard Java logger to which any validation,
   *   tokenization and expansion warnings will be reported, or a null reference
   *   if no logger is present for this schema definition.
   */
  public Logger getLogger();

  /**
   * Creates a new data item which conforms to the schema definition, using the
   * default values specified in the schema definition. The generated data item
   * will be represented in its expanded form.
   *
   * @param includeAll This is a boolean flag which indicates whether the data
   *   item should be generated with optional items included. If set to 'false',
   *   only those schema elements which are specified as being 'required' will be
   *   included.
   * @return Returns a newly generated data item which conforms to the specified
   *   schema definition using the default values.
   */
  public DataItem<?> createDefault(boolean includeAll);

  /**
   * Validates a source data item against the schema definition. If present,
   * validation failures are reported via the schema definition logger.
   *
   * @param sourceDataItem This is the source data item which is to be validated
   *   against the schema definition.
   * @param isTokenized This is a boolean flag which indicates the encoding form
   *   of the source data item. If set to 'true' the source data item is taken to
   *   be in its tokenized form, otherwise validation is carried out on the
   *   assumption that it is in its expanded form.
   * @return Returns a boolean value which will be set to 'true' if the source
   *   data successfully validated against the supplied schema and 'false'
   *   otherwise. The reasons for validation failure will be logged as warnings to
   *   the schema definition object logger, if present.
   */
  public boolean validate(DataItem<?> sourceDataItem, boolean isTokenized);

  /**
   * Converts a source data item from its tokenized form to its expanded form. If
   * present, processing failures are reported via the schema definition logger.
   *
   * @param sourceDataItem This is the tokenized source data item which is to be
   *   expanded using the tokenizing schema.
   * @return Returns an expanded version of the source data item if successful. A
   *   data item with the user data type of {@link UserDataType#UNDEFINED} is
   *   returned on failure, with the decoding status set to
   *   {@link DecodeStatus#FAILED_SCHEMA}. The reasons for expansion failure will
   *   be logged as warnings to the schema definition logger, if present.
   */
  public DataItem<?> expand(DataItem<?> sourceDataItem);

  /**
   * Converts a source data item from its expanded form to its tokenized form. If
   * present, processing failures are reported via the schema definition logger.
   *
   * @param sourceDataItem This is the expanded source data item which is to be
   *   tokenized using the tokenizing schema.
   * @return Returns a tokenized version of the source data item if successful. A
   *   data item with the user data type of {@link UserDataType#UNDEFINED} is
   *   returned on failure, with the decoding status set to
   *   {@link DecodeStatus#FAILED_SCHEMA}. The reasons for tokenization failure
   *   will be logged as warnings to the schema definition logger, if present.
   */
  public DataItem<?> tokenize(DataItem<?> sourceDataItem);

}
