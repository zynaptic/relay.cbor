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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.zynaptic.relay.cbor.MajorType;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides the core implementation for a text string list CBOR data item. This
 * is used to represent a text string which has been split into sections for
 * transmission as an indefinite length text string.
 *
 * @author Chris Holgate
 */
final class TextStringListDataItem extends DataItemCore<List<String>> {

  // This is the list which contains the text segments.
  private final List<String> listData;

  /**
   * Provides a constructor for use by the data item factory API. Creates an
   * empty, mutable list which allows text segments to subsequently be appended.
   *
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  TextStringListDataItem(final int[] tags) {
    super(UserDataType.TEXT_STRING_LIST, tags, true, true);
    listData = new LinkedList<String>();
  }

  /**
   * Provides a constructor for the data item decoder. Creates an immutable list
   * which contains the text string segments decoded from the CBOR binary
   * representation.
   *
   * @param listData This is the list of text string segments which have been
   *   decoded from the CBOR binary representation.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  TextStringListDataItem(final List<String> listData, final int[] tags) {
    super(UserDataType.TEXT_STRING_LIST, tags, false, true);
    this.listData = Collections.unmodifiableList(listData);
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public List<String> getData() {
    return listData;
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {
    writeIndefinitePrimaryData(MajorType.TEXT_STRING, outputStream, getTags());
    for (final String textString : listData) {
      // TODO: Use improved UTF-8 encoding approach.
      final byte[] utf8String = textString.getBytes(StandardCharsets.UTF_8);
      writePrimaryData(MajorType.TEXT_STRING, utf8String.length, outputStream, null);
      outputStream.write(utf8String);
    }
    outputStream.writeByte(BREAK_STOP_CODE);
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    printWriter.print("\"");
    for (final String stringSegment : listData) {
      JsonFormatter.writeJsonString(printWriter, stringSegment);
    }
    printWriter.print("\"");
  }
}
