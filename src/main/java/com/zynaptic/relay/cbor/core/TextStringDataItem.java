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
import java.nio.charset.StandardCharsets;

import com.zynaptic.relay.cbor.MajorType;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides the core implementation for a fixed length text string CBOR data
 * item.
 *
 * @author Chris Holgate
 */
final class TextStringDataItem extends DataItemCore<String> {

  // The native string which represents the CBOR UTF-8 encoded text.
  private final String textString;

  /**
   * Provides the common constructor which is used to create text string data
   * items from both the data item factory API and the CBOR decoder.
   *
   * @param textString This is the text string which corresponds to the CBOR UTF-8
   *   encoded text data item.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  TextStringDataItem(final String textString, final int[] tags) {
    super(UserDataType.TEXT_STRING, tags, false, false);
    this.textString = textString;
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public String getData() {
    return textString;
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {
    // TODO: Use improved UTF-8 encoding approach.
    final byte[] utf8String = textString.getBytes(StandardCharsets.UTF_8);
    writePrimaryData(MajorType.TEXT_STRING, utf8String.length, outputStream, getTags());
    outputStream.write(utf8String);
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    printWriter.print("\"");
    writeJsonString(printWriter, textString);
    printWriter.print("\"");
  }
}
