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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.MajorType;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides the core implementation for the CBOR array data item.
 *
 * @author Chris Holgate
 */
final class ArrayDataItem extends DataItemCore<List<DataItem<?>>> {

  // This is the list of data items which make up the CBOR array.
  private final List<DataItem<?>> arrayData;

  /**
   * Provides a constructor for use by the data item factory API. This creates an
   * empty array data item which can then be populated by application code.
   *
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   * @param indefiniteLength This is a boolean flag which is used to select
   *   whether the CBOR indefinite length format will be used when formatting the
   *   array.
   */
  ArrayDataItem(final int[] tags, final boolean indefiniteLength) {
    super(UserDataType.ARRAY, tags, true, indefiniteLength);
    arrayData = new LinkedList<DataItem<?>>();
  }

  /**
   * Provides a constructor for the data item decoder. This adds an immutable
   * wrapper to the decoded list of data items to prevent it being modified by
   * application code.
   *
   * @param arrayData This is the list of CBOR data items which are to be included
   *   in the array.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   * @param indefiniteLength This is a boolean flag which is used to indicate
   *   whether the decoded CBOR data used indefinite length or fixed length
   *   formatting.
   */
  ArrayDataItem(final List<DataItem<?>> arrayData, final int[] tags, final boolean indefiniteLength) {
    super(UserDataType.ARRAY, tags, false, indefiniteLength);
    this.arrayData = Collections.unmodifiableList(arrayData);
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public List<DataItem<?>> getData() {
    return arrayData;
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {

    // Write out the array using the CBOR indefinite length format.
    if (isIndefiniteLength()) {
      writeIndefinitePrimaryData(MajorType.ARRAY, outputStream, getTags());
      for (final DataItem<?> dataItem : arrayData) {
        ((DataItemCore<?>) dataItem).appendCbor(outputStream);
      }
      outputStream.writeByte(BREAK_STOP_CODE);
    }

    // Write out the array using the CBOR fixed length format.
    else {
      writePrimaryData(MajorType.ARRAY, arrayData.size(), outputStream, getTags());
      for (final DataItem<?> dataItem : arrayData) {
        ((DataItemCore<?>) dataItem).appendCbor(outputStream);
      }
    }
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    boolean firstEntry = true;
    String headString, delimitString, tailString;

    // Selects the text formatting strings to use for compact and human-readable
    // forms.
    if (indent < 0) {
      headString = "[";
      delimitString = ",";
      tailString = "]";
    } else {
      final char[] tabArray = new char[indent];
      Arrays.fill(tabArray, '\t');
      final String tabString = new String(tabArray);
      headString = "[\n\t" + tabString;
      delimitString = ",\n\t" + tabString;
      tailString = "\n" + tabString + "]";
    }

    // Generate the JSON array contents.
    if (arrayData.isEmpty()) {
      printWriter.print('[');
    } else {
      for (final DataItem<?> dataItem : arrayData) {
        final DataItemCore<?> dataItemCore = (DataItemCore<?>) dataItem;
        if (firstEntry) {
          firstEntry = false;
          printWriter.print(headString);
        } else {
          printWriter.print(delimitString);
        }
        dataItemCore.appendJson(printWriter, indent + 1);
      }
    }
    printWriter.print(tailString);
  }
}
