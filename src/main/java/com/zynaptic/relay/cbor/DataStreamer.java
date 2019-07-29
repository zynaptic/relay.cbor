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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Provides support for CBOR and JSON data formatting and decoding. Streaming
 * interfaces are used for formatted data inputs and outputs.
 *
 * @author Chris Holgate
 */
public interface DataStreamer {

  /**
   * Performs CBOR encoding on a CBOR data item. The encoded data item will be
   * appended to the supplied output data stream.
   *
   * @param outputDataItem This is the top-level data item object which is to be
   *   encoded and appended to the supplied output data stream.
   * @param dataOutputStream This is the data output stream to which the encoded
   *   data item will be appended.
   * @throws IOException This exception will be thrown if an I/O error prevented
   *   the output data item from being appended to the output data stream.
   */
  public void encodeCbor(DataItem<?> outputDataItem, DataOutputStream dataOutputStream) throws IOException;

  /**
   * Performs CBOR decoding on a data input stream. The next top-level decoded
   * data item is extracted from the input data stream and returned.
   *
   * @param dataInputStream This is the input stream from which the decoded data
   *   item will be extracted.
   * @return Returns a decoded data item. The status value which is returned by
   *   {@link DataItem#getDecodeStatus()} indicates the success or failure of the
   *   decoding operation. The other data item parameters will only be valid if
   *   calling {@link DecodeStatus#isFailure()} on the status value returns
   *   'false'.
   * @throws IOException This exception will be thrown if an I/O error prevented
   *   the input data stream from being decoded.
   */
  public DataItem<?> decodeCbor(DataInputStream dataInputStream) throws IOException;

  /**
   * Performs JSON encoding on a CBOR data item. The encoded data item will be
   * appended to the supplied output print stream. Elements which cannot be
   * converted to valid JSON are replaced with a null reference. TODO: This does
   * not currently support semantic tagging of data, and as a result no byte array
   * encoding is supported.
   *
   * @param outputDataItem This is the data item object which is to be encoded as
   *   JSON and appended to the supplied output print stream.
   * @param printWriter This is the output print writer to which the encoded data
   *   items will be appended. The associated stream will be automatically flushed
   *   on completion.
   * @param prettify This is a boolean flag which when set to 'true' will format
   *   the generated JSON in a human-readable form.
   * @throws IOException This exception will be thrown if an I/O error prevented
   *   the output data item from being appended to the output data stream.
   */
  public void encodeJson(DataItem<?> outputDataItem, PrintWriter printWriter, boolean prettify) throws IOException;

  /**
   * Performs JSON decoding to CBOR data items on a data input stream. The next
   * top-level decoded data item is extracted from the input stream reader and
   * returned.
   *
   * @param inputStreamReader This is the input stream reader from which the
   *   decoded data items will be extracted.
   * @return Returns a decoded data item. The status value which is returned by
   *   {@link DataItem#getDecodeStatus()} indicates the success or failure of the
   *   decoding operation. The other data item parameters will only be valid if
   *   calling {@link DecodeStatus#isFailure()} on the status value returns
   *   'false'.
   * @throws IOException This exception will be thrown if an I/O error prevented
   *   the input data stream from being decoded.
   */
  public DataItem<?> decodeJson(InputStreamReader inputStreamReader) throws IOException;

}
