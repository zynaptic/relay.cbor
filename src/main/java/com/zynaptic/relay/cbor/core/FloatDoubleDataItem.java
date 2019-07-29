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
 * Provides the core implementation for a 64-bit double precision floating point
 * CBOR data item.
 *
 * @author Chris Holgate
 */
final class FloatDoubleDataItem extends DataItemCore<Double> {

  // This is the double precision floating point value.
  private final double value;

  /**
   * Provides a constructor for use by the data item factory API or JSON decoder.
   *
   * @param value This is the double precision floating point value which is to be
   *   encoded as a CBOR data item.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  FloatDoubleDataItem(final double value, final int[] tags) {
    super(UserDataType.FLOAT_DOUBLE, tags, false, false);
    this.value = value;
  }

  /**
   * Provides a constructor for the CBOR data item decoder. Converts the CBOR
   * encoded floating point representation to a double precision floating point
   * value.
   *
   * @param encodedValue This is the encoded representation which is to be
   *   converted to a double precision floating point value.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  FloatDoubleDataItem(final long encodedValue, final int[] tags) {
    super(UserDataType.FLOAT_DOUBLE, tags, false, false);
    value = Double.longBitsToDouble(encodedValue);
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public Double getData() {
    return value;
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {
    writeExtensionPrimaryData(ExtensionType.FLOAT64, outputStream, getTags());
    outputStream.writeLong(Double.doubleToLongBits(value));
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    if (Double.isInfinite(value) || Double.isNaN(value)) {
      printWriter.print("null");
    } else {
      printWriter.print(value);
    }
  }
}
