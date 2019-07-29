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
import java.util.HashMap;
import java.util.Map;

import com.zynaptic.relay.cbor.DataItem;
import com.zynaptic.relay.cbor.MajorType;
import com.zynaptic.relay.cbor.UserDataType;

/**
 * Provides the core implementation for the CBOR named map data item.
 *
 * @author Chris Holgate
 */
final class NamedMapDataItem extends DataItemCore<Map<String, DataItem<?>>> {

  // This is the map of data items which make up the CBOR named map.
  private final Map<String, DataItem<?>> mapData;

  /**
   * Provides a constructor for use by the data item factory API. This creates an
   * empty named map data item which can then be populated by application code.
   *
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   * @param indefiniteLength This is a boolean flag which is used to select
   *   whether the CBOR indefinite length format will be used when formatting the
   *   map.
   */
  NamedMapDataItem(final int[] tags, final boolean indefiniteLength) {
    super(UserDataType.NAMED_MAP, tags, true, indefiniteLength);
    mapData = new HashMap<String, DataItem<?>>();
  }

  /**
   * Provides a constructor for the data item decoder. This adds an immutable
   * wrapper to the decoded map of data items to prevent it being modified by
   * application code.
   *
   * @param mapData This is the map of text string keys to CBOR data items which
   *   are to be included in the indexed map.
   * @param tags This is an array of integer values which represent the tags to be
   *   applied to the data item.
   * @param indefiniteLength This is a boolean flag which is used to indicate
   *   whether the decoded CBOR data used indefinite length or fixed length
   *   formatting.
   */
  NamedMapDataItem(Map<String, DataItem<?>> mapData, final int[] tags, final boolean indefiniteLength) {
    super(((mapData == null) || (mapData.isEmpty())) ? UserDataType.EMPTY_MAP : UserDataType.NAMED_MAP, tags, false,
        indefiniteLength);
    if (mapData == null) {
      mapData = new HashMap<String, DataItem<?>>(0);
    }
    this.mapData = Collections.unmodifiableMap(mapData);
  }

  /*
   * Implements DataItem.getData()
   */
  @Override
  public Map<String, DataItem<?>> getData() {
    return mapData;
  }

  /*
   * Implements DataItemCore.appendCbor(...)
   */
  @Override
  void appendCbor(final DataOutputStream outputStream) throws IOException {

    // Write out the named map using the CBOR indefinite length format.
    if (isIndefiniteLength()) {
      writeIndefinitePrimaryData(MajorType.MAP, outputStream, getTags());
      for (final Map.Entry<String, DataItem<?>> mapEntry : mapData.entrySet()) {
        final byte[] utf8String = mapEntry.getKey().getBytes("UTF-8");
        writePrimaryData(MajorType.TEXT_STRING, utf8String.length, outputStream, getTags());
        outputStream.write(utf8String);
        ((DataItemCore<?>) mapEntry.getValue()).appendCbor(outputStream);
      }
      outputStream.writeByte(BREAK_STOP_CODE);
    }

    // Write out the named map using the CBOR fixed length format.
    else {
      writePrimaryData(MajorType.MAP, mapData.size(), outputStream, getTags());
      for (final Map.Entry<String, DataItem<?>> mapEntry : mapData.entrySet()) {
        final byte[] utf8String = mapEntry.getKey().getBytes("UTF-8");
        writePrimaryData(MajorType.TEXT_STRING, utf8String.length, outputStream, getTags());
        outputStream.write(utf8String);
        ((DataItemCore<?>) mapEntry.getValue()).appendCbor(outputStream);
      }
    }
  }

  /*
   * Implements DataItemCore.appendJson(...)
   */
  @Override
  void appendJson(final PrintWriter printWriter, final int indent) {
    boolean firstEntry = true;
    String headString, mappingString, delimitString, tailString;

    // Selects the text formatting strings to use for compact and human-readable
    // forms.
    if (indent < 0) {
      headString = "{";
      mappingString = ":";
      delimitString = ",";
      tailString = "}";
    } else {
      final char[] tabArray = new char[indent];
      Arrays.fill(tabArray, '\t');
      final String tabString = new String(tabArray);
      headString = "{\n\t" + tabString;
      mappingString = " : ";
      delimitString = ",\n\t" + tabString;
      tailString = "\n" + tabString + "}";
    }

    // Generate the JSON array contents.
    if (mapData.isEmpty()) {
      printWriter.print('{');
    } else {
      for (final Map.Entry<String, DataItem<?>> mapEntry : mapData.entrySet()) {
        final DataItemCore<?> valueItemCore = (DataItemCore<?>) mapEntry.getValue();
        if (firstEntry) {
          firstEntry = false;
          printWriter.print(headString);
        } else {
          printWriter.print(delimitString);
        }
        printWriter.print("\"" + mapEntry.getKey() + "\"" + mappingString);
        valueItemCore.appendJson(printWriter, indent + 1);
      }
    }
    printWriter.print(tailString);
  }
}
