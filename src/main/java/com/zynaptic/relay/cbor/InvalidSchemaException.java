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

package com.zynaptic.relay.cbor;

/**
 * This exception is thrown if a source data item does not conform to the
 * requirements for a valid schema definition.
 *
 * @author Chris Holgate
 */
public class InvalidSchemaException extends Exception {
  private static final long serialVersionUID = -2728029564041784078L;

  /**
   * Provides standard constructor for a given error message.
   *
   * @param message This is the error message associated with the exception
   *   condition.
   */
  public InvalidSchemaException(final String message) {
    super(message);
  }
}
