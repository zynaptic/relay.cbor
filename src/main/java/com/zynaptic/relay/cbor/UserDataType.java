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
 * Specifies the enumerated set of user data types which may be associated with
 * CBOR data item objects.
 *
 * @author Chris Holgate
 */
public enum UserDataType {

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a CBOR array. Data items with this user data type will always be
   * parameterised with type <code>T&nbsp=&nbspList&ltDataItem&lt?&gt&gt</code>.
   */
  ARRAY,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a decoded CBOR map which contains no entries. Data items with this user data
   * type will always be parameterised with type
   * <code>T&nbsp=&nbspMap&ltString,&nbspDataItem&lt?&gt&gt</code>.
   */
  EMPTY_MAP,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a CBOR map with integer keys. The range of key values is restricted to those
   * that can be encoded using 64-bit 2's complement notation (ie, a Java long
   * integer). Data items with this user data type will always be parameterised
   * with type <code>T&nbsp=&nbsp&ltLong,&nbspDataItem&lt?&gt&gt</code>.
   */
  INDEXED_MAP,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a CBOR map with text string keys. The keys will always be encoded as fixed
   * length CBOR text strings. Data items with this user data type will always be
   * parameterised with type
   * <code>T&nbsp=&nbspMap&ltString,&nbspDataItem&lt?&gt&gt</code>.
   */
  NAMED_MAP,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * an integer value. The range of data values is restricted to those that can be
   * encoded using 64-bit 2's complement notation (ie, a Java long integer). Data
   * items with this user data type will always be parameterised with type
   * <code>T&nbsp=&nbspLong</code>.
   */
  INTEGER,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a fixed length CBOR text string. Data items with this user data type will
   * always be parameterised with type <code>T&nbsp=&nbspString</code>.
   */
  TEXT_STRING,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * an indefinite length CBOR text string. This is represented as a list of Java
   * strings, with each one corresponding to a constituent CBOR text string
   * segment. Data items with this user data type will always be parameterised
   * with type <code>T&nbsp=&nbspList&ltString&gt</code>.
   */
  TEXT_STRING_LIST,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a fixed length CBOR byte string. Data items with this user data type will
   * always be parameterised with type <code>T&nbsp=&nbspbyte[]</code>.
   */
  BYTE_STRING,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * an indefinite length CBOR byte string. This is represented as a list of Java
   * byte arrays, with each one corresponding to a constituent CBOR byte string
   * segment. Data items with this user data type will always be parameterised
   * with type <code>T&nbsp=&nbspList&ltbyte[]&gt</code>.
   */
  BYTE_STRING_LIST,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a 16-bit half precision floating point value. Data items with this user data
   * type will always be parameterised with type <code>T&nbsp=&nbspFloat</code>.
   * On conversion from Java native floating point values to the restricted half
   * precision format, any values outside the -65504 to +65504 maximum range will
   * be mapped to the appropriate infinite value representation.
   */
  FLOAT_HALF,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a 32-bit standard precision floating point value. Data items with this user
   * data type will always be parameterised with type
   * <code>T&nbsp=&nbspFloat</code>.
   */
  FLOAT_STANDARD,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a 64-bit double precision floating point value. Data items with this user
   * data type will always be parameterised with type
   * <code>T&nbsp=&nbspDouble</code>.
   */
  FLOAT_DOUBLE,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a boolean value. Data items with this user data type will always be
   * parameterised with type <code>T&nbsp=&nbspBoolean</code>.
   */
  BOOLEAN,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * an undefined value. Data items of this type will always be parameterised with
   * type <code>T&nbsp=&nbspBoolean</code> and will always return a value of
   * 'false' via the {@link DataItem#getData()} and {@link DataItem#castData()}
   * methods.
   */
  UNDEFINED,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * a null value. Data items of this type will always be parameterised with type
   * <code>T&nbsp=&nbspBoolean</code> and will always return return a value of
   * 'false' via the {@link DataItem#getData()} and {@link DataItem#castData()}
   * methods.
   */
  NULL,

  /**
   * This user data type is used to indicate that a data item object encapsulates
   * one of the unassigned simple data values. Data items with this user data type
   * will always be parameterised with type <code>T&nbsp=&nbspInteger</code>.
   */
  SIMPLE

}
