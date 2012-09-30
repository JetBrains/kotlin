// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.common;

import com.google.dart.compiler.Source;

/**
 * Abstract view of a class that has source info.
 */
public interface HasSourceInfo {

  /**
   * Return the source info associated with this object.
   */
  SourceInfo getSourceInfo();

  /**
   * Set the source info associated with this object. May only be called once.
   * @param info
   */
  void setSourceInfo(SourceInfo info);

  /**
   * Sets the source range of the original source file where the source fragment
   * corresponding to this node was found.
   * 
   * <p>
   * Each node in the subtree (other than the contrived nodes) carries source
   * range(s) information relating back to positions in the given source (the
   * given source itself is not remembered with the AST). The source range
   * usually begins at the first character of the first token corresponding to
   * the node; leading whitespace and comments are <b>not</b> included. The
   * source range usually extends through the last character of the last token
   * corresponding to the node; trailing whitespace and comments are <b>not</b>
   * included. There are a handful of exceptions (including the various body
   * declarations). Source ranges nest properly: the source range for a child is
   * always within the source range of its parent, and the source ranges of
   * sibling nodes never overlap.
   * 
   * @param source the associated source
   * @param line the 1-based line index, or <code>-1</code>, if no source
   *        location is available
   * @param column the 1-based column index, or <code>-1</code>, if no source
   *        location is available
   * @param startPosition a 0-based character index, or <code>-1</code>, if no
   *        source location is available
   * @param length a (possibly 0) length, or <code>-1</code>, if no source
   *        location is available
   * @see SourceInfo#getStart()
   * @see SourceInfo#getLength()
   */
  void setSourceLocation(Source source, int line, int column, int startPosition, int length);
}
