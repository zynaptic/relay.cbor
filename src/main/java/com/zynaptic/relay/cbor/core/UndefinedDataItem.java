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

import com.zynaptic.relay.cbor.DecodeStatus;
import com.zynaptic.relay.cbor.ExtensionType;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides the core implementation for the CBOR undefined and null data items.
 *
 * @author Chris Holgate
 */
final class UndefinedDataItem extends DataItemCore<Boolean> {

  /**
   * Provides a statically defined instance of an invalid data item.
   */
  static UndefinedDataItem INVALID = new UndefinedDataItem(DecodeStatus.INVALID);

  /**
   * Provides a statically defined instance of an unsupported data item.
   */
  static UndefinedDataItem UNSUPPORTED = new UndefinedDataItem(DecodeStatus.UNSUPPORTED);

  /**
   * Provides a statically defined instance of a failed schema data item.
   */
  static UndefinedDataItem FAILED_SCHEMA = new UndefinedDataItem(DecodeStatus.FAILED_SCHEMA);

  /**
   * Provides the simplified constructor for null data items with a specified
   * decoding error status.
   *
   * @param decodeStatus This is the decoding status which is associated with the
   *   new data item.
   */
  UndefinedDataItem(final DecodeStatus decodeStatus) {
    super(UserDataType.NULL, null, false, false);
    setDecodeStatus(decodeStatus);
  }

  /**
   * Provides the common constructor which is used to create undefined and null
   * data items from both the data item factory API and the CBOR decoder.
   *
   * @param isNull This is a boolean value which when set to 'true' indicates that
   *   the undefined data is to be characterised as a null data item.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  UndefinedDataItem(final boolean isNull, final int[] tags) {
    super(isNull ? UserDataType.NULL : UserDataType.UNDEFINED, tags, false, false);
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public Boolean getData() {
    return false;
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {
    final ExtensionType extensionType = (getDataType() == UserDataType.NULL) ? ExtensionType.NULL
        : ExtensionType.UNDEFINED;
    writeExtensionPrimaryData(extensionType, outputStream, getTags());
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    printWriter.print("null");
  }
}
