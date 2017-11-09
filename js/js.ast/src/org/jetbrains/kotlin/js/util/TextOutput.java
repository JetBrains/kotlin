// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.util;

/**
 * Interface used for printing text output.
 */
public interface TextOutput {
    int getPosition();

    int getLine();

    int getColumn();

    void indentIn();

    void indentOut();

    void newline();

    void print(char c);

    void print(int v);

    void print(double v);

    void print(char[] s);

    void print(CharSequence s);

    void printOpt(char c);

    boolean isCompact();

    void maybeIndent();
}
