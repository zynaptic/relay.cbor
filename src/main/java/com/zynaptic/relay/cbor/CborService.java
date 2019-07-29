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
   * Accesses the CBOR tokenizing schema service interface, which may be used for
   * automatically tokenizing and expanding JSON and CBOR messages using formal
   * schema definitions.
   *
   * @return Returns a reference to the CBOR tokenizing schema service interface
   *   to be used for processing JSON and CBOR messages using formal schema
   *   definitions.
   */
  // TODO: public CborSchemaService getCborSchemaService();

}
