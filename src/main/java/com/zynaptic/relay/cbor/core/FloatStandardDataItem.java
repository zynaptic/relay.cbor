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

import com.zynaptic.relay.cbor.ExtensionType;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides the core implementation for a 32-bit single precision floating point
 * CBOR data item.
 *
 * @author Chris Holgate
 */
final class FloatStandardDataItem extends DataItemCore<Float> {

  // This is the single precision floating point value.
  private final float value;

  /**
   * Provides a constructor for use by the data item factory API.
   *
   * @param value This is the single precision floating point value which is to be
   *   encoded as a CBOR data item.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  FloatStandardDataItem(final float value, final int[] tags) {
    super(UserDataType.FLOAT_STANDARD, tags, false, false);
    this.value = value;
  }

  /**
   * Provides a constructor for the data item decoder. Converts the CBOR encoded
   * floating point representation to a single precision floating point value.
   *
   * @param encodedValue This is the encoded representation which is to be
   *   converted to a single precision floating point value.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  FloatStandardDataItem(final int encodedValue, final int[] tags) {
    super(UserDataType.FLOAT_STANDARD, tags, false, false);
    value = Float.intBitsToFloat(encodedValue);
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public Float getData() {
    return value;
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {
    writeExtensionPrimaryData(ExtensionType.FLOAT32, outputStream, getTags());
    outputStream.writeInt(Float.floatToIntBits(value));
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    if (Float.isInfinite(value) || Float.isNaN(value)) {
      printWriter.print("null");
    } else {
      printWriter.print(value);
    }
  }
}
