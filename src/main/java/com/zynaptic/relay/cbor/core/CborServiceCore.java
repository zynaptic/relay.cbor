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

import com.zynaptic.relay.cbor.CborService;
import com.zynaptic.relay.cbor.DataItemFactory;
import com.zynaptic.relay.cbor.DataStreamer;

/**
 * Provides the core implementation of the CBOR service interface, which may be
 * used for accessing the data item factory and data streamer component.
 *
 * @author Chris Holgate
 */
public final class CborServiceCore implements CborService {

  private final DataItemFactory dataItemFactory = new DataItemFactoryCore();
  private final DataStreamer dataStreamer = new DataStreamerCore();

  /*
   * Implements CborService.getDataItemFactory()
   */
  @Override
  public DataItemFactory getDataItemFactory() {
    return dataItemFactory;
  }

  /*
   * Implements CborService.getDataStreamer()
   */
  @Override
  public DataStreamer getDataStreamer() {
    return dataStreamer;
  }
}
