/*
 * Zynaptic CBOR library - An RFC7049 based data serialization library.
 *
 * Copyright (c) 2015, Zynaptic Limited. All rights reserved. DO NOT ALTER OR
 * REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 only, as published by
 * the Free Software Foundation. Zynaptic Limited designates this particular
 * file as subject to the "Classpath" exception as provided in the LICENSE file
 * that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License version 2 for more
 * details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please visit www.zynaptic.com or contact buzz@zynaptic.com if you need
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
