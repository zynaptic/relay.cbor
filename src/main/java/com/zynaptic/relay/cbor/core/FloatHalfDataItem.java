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
 * Provides the core implementation for a 16-bit half precision floating point
 * CBOR data item.
 * <p>
 * The half precision floating point conversion routines were based on those
 * provided and placed in the public domain by the following contributor to
 * StackOverflow: <a href =
 * http://stackoverflow.com/questions/6162651/half-precision-floating-point
 * -in-java/6162687#6162687>"Half-precision floating-point in Java"</a>.
 *
 * @author Chris Holgate
 */
final class FloatHalfDataItem extends DataItemCore<Float> {

  // The encoded half precision data value.
  private final int encodedValue;

  /**
   * Provides a constructor for use by the data item factory API.
   *
   * @param value This is the single precision floating point value which is to be
   *   encoded as a half precision CBOR data item.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  FloatHalfDataItem(final float value, final int[] tags) {
    super(UserDataType.FLOAT_HALF, tags, false, false);
    encodedValue = encodeFromFloat(value);
  }

  /**
   * Provides a constructor for the data item decoder.
   *
   * @param encodedValue This is the encoded representation which is to be
   *   converted to a half precision floating point value.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   */
  FloatHalfDataItem(final int encodedValue, final int[] tags) {
    super(UserDataType.FLOAT_HALF, tags, false, false);
    this.encodedValue = encodedValue;
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public Float getData() {
    return decodeToFloat(encodedValue);
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {
    writeExtensionPrimaryData(ExtensionType.FLOAT16, outputStream, getTags());
    outputStream.writeShort(encodedValue);
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    final float value = decodeToFloat(encodedValue);
    if (Float.isInfinite(value) || Float.isNaN(value)) {
      printWriter.print("null");
    } else {
      printWriter.print(value);
    }
  }

  /*
   * Encodes the specified floating point value using the IEEE 754 half precision
   * format.
   */
  private int encodeFromFloat(final float fval) {
    final int fbits = Float.floatToIntBits(fval);

    // Extract sign and rounded absolute value.
    final int sign = fbits >>> 16 & 0x8000;
    int val = (fbits & 0x7fffffff) + 0x1000;

    // Map to NaN/Inf, including the optimisation to avoid rounding to infinity
    // as introduced by the original author.
    if (val >= 0x47800000) {

      // Is or must become NaN/Inf.
      if ((fbits & 0x7fffffff) >= 0x47800000) {
        if (val < 0x7f800000) {
          return sign | 0x7c00;
        }
        return sign | 0x7c00 | (fbits & 0x007fffff) >>> 13;
      }
      return sign | 0x7bff;
    }

    // Remains a normalized value with implicit rounding to reduced precision.
    if (val >= 0x38800000) {
      return sign | val - 0x38000000 >>> 13;
    }

    // Map values less than subnormal to +/-0.
    if (val < 0x33000000) {
      return sign;
    }

    // Perform subnormal encoding for reduced exponent range.
    val = (fbits & 0x7fffffff) >>> 23;
    return sign | ((fbits & 0x7fffff | 0x800000) + (0x800000 >>> val - 102) >>> 126 - val);
  }

  /*
   * Decodes the floating point value from the IEEE 754 half precision format.
   */
  private float decodeToFloat(final int hbits) {

    // Extracts 10 bits mantissa and 5 bits exponent.
    int mant = hbits & 0x03ff;
    int exp = hbits & 0x7c00;

    // Map NaN/Inf encodings to 32-bit form.
    if (exp == 0x7c00) {
      exp = 0x3fc00;
    } else if (exp != 0) {
      exp += 0x1c000;
      if (mant == 0 && exp > 0x1c400) {
        return Float.intBitsToFloat((hbits & 0x8000) << 16 | exp << 13 | 0x3ff);
      }
    }

    // Process subnormal values, rescaling the mantissa and exponent until
    // normalized and then discarding the subnormal bit.
    else if (mant != 0) {
      exp = 0x1c400;
      do {
        mant <<= 1;
        exp -= 0x400;
      } while ((mant & 0x400) == 0);
      mant &= 0x3ff;
    }

    // Combine all parts, including +/-0.
    return Float.intBitsToFloat((hbits & 0x8000) << 16 | (exp | mant) << 13);
  }
}
