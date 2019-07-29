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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.zynaptic.relay.cbor.MajorType;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides the core implementation for the indefinite length CBOR byte string
 * data item. This treats indefinite length CBOR byte strings as if they were a
 * list of substrings.
 *
 * @author Chris Holgate
 */
final class ByteStringListDataItem extends DataItemCore<List<byte[]>> {

  // This is the list of CBOR byte substrings.
  private final List<byte[]> listData;

  /**
   * Provides a constructor for use by the data item factory API. This creates an
   * empty byte string list data item which can then be populated by application
   * code.
   *
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  ByteStringListDataItem(final int[] tags) {
    super(UserDataType.BYTE_STRING_LIST, tags, true, true);
    listData = new LinkedList<byte[]>();
  }

  /**
   * Provides a constructor for the data item decoder. This adds an immutable
   * wrapper to the decoded list of byte substrings to prevent it being modified
   * by application code.
   *
   * @param listData This is the list of byte substrings which have been extracted
   *   by the decoder.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   * @param decodeStatus This is the decoding status which is associated with the
   *   new data item.
   */
  ByteStringListDataItem(final List<byte[]> listData, final int[] tags) {
    super(UserDataType.BYTE_STRING_LIST, tags, false, true);
    this.listData = Collections.unmodifiableList(listData);
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public List<byte[]> getData() {
    return listData;
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {
    writeIndefinitePrimaryData(MajorType.BYTE_STRING, outputStream, getTags());
    for (final byte[] byteString : listData) {
      writePrimaryData(MajorType.BYTE_STRING, byteString.length, outputStream, null);
      outputStream.write(byteString);
    }
    outputStream.writeByte(BREAK_STOP_CODE);
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    printWriter.print("\"");
    JsonFormatter.writeBase64Url(printWriter, listData);
    printWriter.print("\"");
  }
}
