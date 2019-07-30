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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DataStreamer;

/**
 * Provides core implementation of the CBOR and JSON data streamer interface.
 *
 * @author Chris Holgate
 */
final class DataStreamerCore implements DataStreamer {

  /*
   * Implements DataStreamer.encodeCbor(...)
   */
  @Override
  public void encodeCbor(final DataItem<?> outputDataItem, final DataOutputStream dataOutputStream) throws IOException {
    final DataItemCore<?> dataItemCore = (DataItemCore<?>) outputDataItem;
    dataItemCore.appendCbor(dataOutputStream);
  }

  /*
   * Implements DataStreamer.decodeCbor(..)
   */
  @Override
  public DataItem<?> decodeCbor(final DataInputStream dataInputStream) throws IOException {
    return new CborItemDecoder(dataInputStream).decode();
  }

  /*
   * Implements DataStreamer.encodeJson(...)
   */
  @Override
  public void encodeJson(final DataItem<?> outputDataItem, final PrintWriter printWriter, final boolean prettify)
      throws IOException {
    final DataItemCore<?> dataItemCore = (DataItemCore<?>) outputDataItem;
    dataItemCore.appendJson(printWriter, prettify ? 0 : -1);
    if (prettify) {
      printWriter.println();
    }
    printWriter.flush();
  }

  /*
   * Implements DataStreamer.decodeJson(...)
   */
  @Override
  public DataItem<?> decodeJson(final InputStreamReader inputStreamReader) throws IOException {
    return new JsonItemDecoder(inputStreamReader).decode();
  }
}
