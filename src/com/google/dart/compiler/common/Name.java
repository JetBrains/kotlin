// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Instead of Strings, we use Names to abstract the underlying representation.
 * 
 * <ul>
 * <li>Names are globally unique and use identity for equality.</li>
 * <li>Names are interned, so all references to the same name use the same
 * bytes.</li>
 * </ul>
 * TODO(scottb): use byte[] instead of char[] when the parser is rewritten
 * to parse byte[].
 */
public final class Name implements Serializable {

  /**
   * The encoding this class uses when converting between chars and bytes.
   */
  public static final Charset CHARSET = Charset.forName("UTF-8");

  private static final NameFactory factory = new NameFactory();

  private static final long serialVersionUID = 0L;

  /**
   * Return the Name corresponding to the data. An internal reference to
   * <code>data</code> is kept for efficiency, do NOT mutate data after calling
   * this method.
   */
  public static Name of(char[] data) {
    return factory.of(data);
  }

  /**
   * Return the Name corresponding to the data. An internal copy of the data is
   * made.
   */
  public static Name of(char[] data, int offset, int length) {
    return factory.of(data, offset, length);
  }

  static int computeHashCode(char[] data, int offset, int length) {
    // Effective Java Item 9.
    int result = 89;
    for (int i = offset, end = offset + length; i < end; ++i) {
      result *= 31;
      result += data[i];
    }
    return result;
  }

  final char[] data;

  private final transient int hashCode;

  Name(char[] data, int hashCode) {
    this.data = data;
    this.hashCode = hashCode;
  }

  /**
   * Always compares based on identity.
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  /**
   * Returns the hashCode of the underlying data.
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * Constructs a String to represent the internal data.
   */
  @Override
  public String toString() {
    return String.valueOf(data);
  }

  /**
   * Write my data into a {@link java.io.OutputStream} using the encoding specified in
   * {@link #CHARSET}.
   */
  public void writeBytesTo(OutputStream out) throws IOException {
    // TODO(scottb): avoid allocating the String.
    out.write(new String(data).getBytes(CHARSET));
  }

  /**
   * Write my character data into a {@link java.io.PrintStream}.
   */
  public void writeCharsTo(PrintStream out) {
    out.print(data);
  }

  /**
   * Write my character data into a {@link java.io.Writer}.
   */
  public void writeCharsTo(Writer writer) throws IOException {
    writer.write(data);
  }

  /**
   * Replace with the canonical instance.
   */
  private Object readResolve() {
    return Name.of(data);
  }
}
