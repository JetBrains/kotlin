// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Adapts {@link TextOutput} to an internal text buffer.
 */
public class DefaultTextOutput extends AbstractTextOutput {

  private final StringWriter sw = new StringWriter();
  private final PrintWriter out;

  public DefaultTextOutput(boolean compact) {
    super(compact);
    setPrintWriter(out = new PrintWriter(sw));
  }

  @Override
  public String toString() {
    out.flush();
    if (sw != null) {
      return sw.toString();
    } else {
      return super.toString();
    }
  }
}
