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
 * Provides the core implementation for a CBOR integer data item. The range of
 * valid integer values is restricted by this implementation to those that can
 * be represented in 64-bit 2's complement form (ie, by a Java long integer
 * type).
 *
 * @author Chris Holgate
 */
final class IntegerDataItem extends DataItemCore<Long> {

  // This is the 64-bit 2's complement integer value.
  private final long value;

  /**
   * Provides the common constructor which is used to create integer data items
   * from both the data item factory API and the CBOR decoder.
   *
   * @param value This is the 64-bit 2's complement integer data value.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  IntegerDataItem(final long value, final int[] tags) {
    super(UserDataType.INTEGER, tags, false, false);
    this.value = value;
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public Long getData() {
    return value;
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {
    MajorType majorType;
    long magnitude;
    if (value >= 0) {
      majorType = MajorType.UNSIGNED;
      magnitude = value;
    } else {
      majorType = MajorType.NEGATIVE;
      magnitude = ~value;
    }
    writePrimaryData(majorType, magnitude, outputStream, getTags());
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    printWriter.print(value);
  }
}
