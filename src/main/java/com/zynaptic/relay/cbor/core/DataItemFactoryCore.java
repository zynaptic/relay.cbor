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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.DataItemFactory;
import com.zynaptic.relay.cbor.DecodeStatus;

/**
 * Provides the core implementation of the CBOR data item factory interface,
 * which may be used for programmatically generating CBOR messages.
 *
 * @author Chris Holgate
 */
public final class DataItemFactoryCore implements DataItemFactory {

  /*
   * Implements DataItemFactory.createInvalidItem(...)
   */
  @Override
  public DataItem<?> createInvalidItem(final DecodeStatus decodeStatus) {
    return new UndefinedDataItem(decodeStatus);
  }

  /*
   * Implements DataItemFactory.createBooleanItem(...)
   */
  @Override
  public DataItem<Boolean> createBooleanItem(final boolean value, final int[] tags) {
    return new BooleanDataItem(value, tags);
  }

  /*
   * Implements DataItemFactory.createUndefinedItem(...)
   */
  @Override
  public DataItem<Boolean> createUndefinedItem(final boolean isNull, final int[] tags) {
    return new UndefinedDataItem(isNull, tags);
  }

  /*
   * Implements DataItemFactory.createSimpleDataItem(...)
   */
  @Override
  public DataItem<Integer> createSimpleDataItem(final int value, final int[] tags) {
    if ((value < 0) || (value > 255) || ((value >= 20) && (value <= 31))) {
      throw new IllegalArgumentException("Simple data value must be one of the RFC7049 unassigned values.");
    }
    return new SimpleDataItem(value, tags);
  }

  /*
   * Implements DataItemFactory.createIntegerItem(...)
   */
  @Override
  public DataItem<Long> createIntegerItem(final long value, final int[] tags) {
    return new IntegerDataItem(value, tags);
  }

  /*
   * Implements DataItemFactory.createHpFloatItem(...)
   */
  @Override
  public DataItem<Float> createHpFloatItem(final float value, final int[] tags) {
    return new FloatHalfDataItem(value, tags);
  }

  /*
   * Implements DataItemFactory.createSpFloatItem(...)
   */
  @Override
  public DataItem<Float> createSpFloatItem(final float value, final int[] tags) {
    return new FloatStandardDataItem(value, tags);
  }

  /*
   * Implements DataItemFactory.createDpFloatItem(...)
   */
  @Override
  public DataItem<Double> createDpFloatItem(final double value, final int[] tags) {
    return new FloatDoubleDataItem(value, tags);
  }

  /*
   * Implements DataItemFactory.createTextStringItem(...)
   */
  @Override
  public DataItem<String> createTextStringItem(final String textString, final int[] tags) {
    return new TextStringDataItem(textString, tags);
  }

  /*
   * Implements DataItemFactory.createTextStringListItem(...)
   */
  @Override
  public DataItem<List<String>> createTextStringListItem(final int[] tags) {
    return new TextStringListDataItem(tags);
  }

  /*
   * Implements DataItemFactory.createByteStringItem(...)
   */
  @Override
  public DataItem<byte[]> createByteStringItem(final byte[] byteString, final int[] tags) {
    return new ByteStringDataItem(byteString, tags);
  }

  /*
   * Implements DataItemFactory.createByteStringItem(...)
   */
  @Override
  public DataItem<byte[]> createByteStringItem(final String encodedString, final int[] tags) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      if (Base64Encoding.decodeBase64(outputStream, encodedString, false)) {
        return new ByteStringDataItem(outputStream.toByteArray(), tags);
      } else {
        return new ByteStringDataItem(null, tags).setDecodeStatus(DecodeStatus.INVALID);
      }
    } catch (final IOException error) {
      return new ByteStringDataItem(null, tags).setDecodeStatus(DecodeStatus.INVALID);
    }
  }

  /*
   * Implements DataItemFactory.createByteStringListItem(...)
   */
  @Override
  public DataItem<List<byte[]>> createByteStringListItem(final int[] tags) {
    return new ByteStringListDataItem(tags);
  }

  /*
   * Implements DataItemFactory.createArrayItem(...)
   */
  @Override
  public DataItem<List<DataItem<?>>> createArrayItem(final int[] tags, final boolean indefiniteLength) {
    return new ArrayDataItem(tags, indefiniteLength);
  }

  /*
   * Implements DataItemFactory.createIndexedMapItem(...)
   */
  @Override
  public DataItem<Map<Long, DataItem<?>>> createIndexedMapItem(final int[] tags, final boolean indefiniteLength) {
    return new IndexedMapDataItem(tags, indefiniteLength);
  }

  /*
   * Implements DataItemFactory.createNamedMapItem(...)
   */
  @Override
  public DataItem<Map<String, DataItem<?>>> createNamedMapItem(final int[] tags, final boolean indefiniteLength) {
    return new NamedMapDataItem(tags, indefiniteLength);
  }
}
