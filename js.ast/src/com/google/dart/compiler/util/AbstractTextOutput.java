// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.util;

import java.io.PrintWriter;
import java.util.Arrays;

/**
 * An abstract base type to build TextOutput implementations.
 */
public abstract class AbstractTextOutput implements TextOutput {
  private final boolean compact;
  private int identLevel = 0;
  private int indentGranularity = 2;
  private char[][] indents = new char[][] {new char[0]};
  private boolean justNewlined;
  private PrintWriter out;
  private int position = 0;
  private int line = 0;
  private int column = 0;

  protected AbstractTextOutput(boolean compact) {
    this.compact = compact;
  }

  public int getPosition() {
    return position;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public void indentIn() {
    ++identLevel;
    if (identLevel >= indents.length) {
      // Cache a new level of indentation string.
      //
      char[] newIndentLevel = new char[identLevel * indentGranularity];
      Arrays.fill(newIndentLevel, ' ');
      char[][] newIndents = new char[indents.length + 1][];
      System.arraycopy(indents, 0, newIndents, 0, indents.length);
      newIndents[identLevel] = newIndentLevel;
      indents = newIndents;
    }
  }

  public void indentOut() {
    --identLevel;
  }

  public void newline() {
    out.print('\n');
    position++;
    line++;
    column = 0;
    justNewlined = true;
  }

  public void newlineOpt() {
    if (!compact) {
      newline();
    }
  }

  public void print(char c) {
    maybeIndent();
    out.print(c);
    position++;
    column++;
    justNewlined = false;
  }

  public void print(char[] s) {
    maybeIndent();
    printAndCount(s);
    justNewlined = false;
  }

  public void print(String s) {
    maybeIndent();
    printAndCount(s.toCharArray());
    justNewlined = false;
  }

  // Why don't the "Opt" methods update "justNewLined"?
  public void printOpt(char c) {
    if (!compact) {
      maybeIndent();
      out.print(c);
      position += 1;
      column++;
    }
  }

  public void printOpt(char[] s) {
    if (!compact) {
      maybeIndent();
      printAndCount(s);
    }
  }

  public void printOpt(String s) {
    if (!compact) {
      maybeIndent();
      printAndCount(s.toCharArray());
    }
  }

  protected void setPrintWriter(PrintWriter out) {
    this.out = out;
  }

  private void maybeIndent() {
    if (justNewlined && !compact) {
      printAndCount(indents[identLevel]);
      justNewlined = false;
    }
  }

  private void printAndCount(char[] chars) {
    position += chars.length;
    column += chars.length;
    out.print(chars);
  }
}
