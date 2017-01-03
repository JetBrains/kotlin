// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.common;

import com.google.dart.compiler.Source;

import java.io.Serializable;

/**
 * Tracks file and line information for AST nodes.
 */
public interface SourceInfo extends Serializable {

  /**
   * The source code provider.
   */
  Source getSource();

  /**
   * @return A 1-based line number into the original source file indicating
   * where the source fragment begins.
   */
  int getLine();

  /**
   * @return A 1-based column number into the original source file indicating
   * where the source fragment begins.
   */
  int getColumn();

  /**
   * Returns the character index into the original source file indicating
   * where the source fragment corresponding to this node begins.
   * 
   * <p>
   * The parser supplies useful well-defined source ranges to the nodes it creates.
   *
   * @return the 0-based character index, or <code>-1</code>
   *    if no source startPosition information is recorded for this node
   * @see #getLength()
   * @see HasSourceInfo#setSourceLocation(Source, int, int, int, int)
   */
  int getStart();

  /**
   * Returns the length in characters of the original source file indicating
   * where the source fragment corresponding to this node ends.
   * <p>
   * The parser supplies useful well-defined source ranges to the nodes it creates.
   *
   * @return a (possibly 0) length, or <code>0</code>
   *    if no source source position information is recorded for this node
   * @see #getStart()
   * @see HasSourceInfo#setSourceLocation(Source, int, int, int, int)
   */
  int getLength();
}
