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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.zynaptic.relay.cbor.MajorType;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides the core implementation for the fixed length CBOR byte string data
 * item.
 *
 * @author Chris Holgate
 */
final class ByteStringDataItem extends DataItemCore<byte[]> {

  // This is the CBOR byte string data value.
  private final byte[] byteString;

  /**
   * Provides the common constructor which is used to create byte string data
   * items from both the data item factory API and the CBOR decoder.
   *
   * @param byteString This is the CBOR byte string data value.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  ByteStringDataItem(final byte[] byteString, final int[] tags) {
    super(UserDataType.BYTE_STRING, tags, false, false);
    this.byteString = byteString;
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public byte[] getData() {
    return byteString.clone();
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {
    writePrimaryData(MajorType.BYTE_STRING, byteString.length, outputStream, getTags());
    outputStream.write(byteString);
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    printWriter.print("\"");
    Base64Encoding.writeBase64Url(printWriter, byteString);
    printWriter.print("\"");
  }
}
