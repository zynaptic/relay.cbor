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
 * Specifies the enumerated set of decoding status values which can be generated
 * during the CBOR decoding operation.
 *
 * @author Chris Holgate
 */
public enum DecodeStatus {

  /**
   * Indicates that the CBOR message contained RFC7049 defined formatting errors
   * and could not be processed.
   */
  INVALID(0),

  /**
   * Indicates that the CBOR message could not be processed due to an
   * implementation specific restriction. This includes situations where failure
   * was due to the following limitations:
   * <ul>
   * <li>Integer values as represented by the unsigned and negative major data
   * types are restricted to values that can be represented using 64-bit 2's
   * complement (ie, Java long integers).
   * <li>The only data types which may be used for map keys in conjunction with
   * this implementation are integer values that can be represented using 64-bit
   * 2's complement (ie, Java long integers) and UTF-8 strings. All keys in a
   * given map must be of the same type.
   * <li>The range of available tags is restricted to values that can be
   * represented as a non-negative integer using 32-bit 2's complement (ie, Java
   * positive integers).
   * <li>The total length for all text strings, byte strings, arrays and maps is
   * restricted to values that can be represented as a non-negative integer using
   * 32-bit 2's complement (ie, Java positive integers).
   * </ul>
   */
  UNSUPPORTED(1),

  /**
   * Indicates that an attempt has been made to validate a CBOR message against a
   * given schema and the validation failed due to a mismatch between the message
   * contents and the schema specification. This also applies to failed attempts
   * to tokenize or expand a given message, since validation is always carried out
   * during these processes.
   */
  FAILED_SCHEMA(2),

  /**
   * Indicates that the message was well formed and could be parsed. However,
   * there may be inconsistencies between different decoders as a result of the
   * use of unrecognised simple values or duplicate map keys (see RFC7049 section
   * 3.7).
   */
  WELL_FORMED(3),

  /**
   * Indicates that a successful attempt has been made to tokenize a message using
   * a given tokenizing schema.
   */
  TOKENIZED(4),

  /**
   * Indicates that a successful attempt has been made to expand a message using a
   * given tokenizing schema.
   */
  EXPANDED(5),

  /**
   * Indicates that the message was well formed, with unique keys for all of the
   * decoded map entries. This ensures that the CBOR data can be unambiguously
   * translated to JSON format.
   */
  TRANSLATABLE(6),

  /**
   * Indicates that the message is in its original form, as programmatically
   * assembled using the CBOR base service API.
   */
  ORIGINAL(7);

  /**
   * Performs status priority selection. Given a new status value to compare
   * against the current priority level, selects the one which corresponds to the
   * least strict decoding criteria.
   *
   * @param newStatus This is the new decoding status value which is to be
   *   compared against the current priority level.
   * @return Returns the decoding status which corresponds to the least strict
   *   decoding criteria.
   */
  public DecodeStatus getLeastStrictCriteria(final DecodeStatus newStatus) {
    return (newStatus.priorityLevel < priorityLevel) ? newStatus : this;
  }

  /**
   * Indicates whether the decoding status value corresponds to a failure
   * condition, in which case no valid decoded data is available.
   *
   * @return Returns a boolean value which will be set to 'true' if the decoding
   *   status value corresponds to a failure condition.
   */
  public boolean isFailure() {
    return ((this == DecodeStatus.INVALID) || (this == DecodeStatus.UNSUPPORTED)
        || (this == DecodeStatus.FAILED_SCHEMA));
  }

  // Specifies the priority level associated with the decoding status.
  private final int priorityLevel;

  /*
   * Default constructor initialises the decoder status priority level.
   */
  private DecodeStatus(final int priorityLevel) {
    this.priorityLevel = priorityLevel;
  }
}