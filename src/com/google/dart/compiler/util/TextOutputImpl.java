// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.util;

import java.util.Arrays;

/**
 * An abstract base type to build TextOutput implementations.
 */
public class TextOutputImpl implements TextOutput {
    private final boolean compact;
    private int identLevel = 0;
    private final static int indentGranularity = 2;
    private char[][] indents = new char[][] {new char[0]};
    private boolean justNewlined;
    private final StringBuilder out;
    private int position = 0;
    private int line = 0;
    private int column = 0;

    public TextOutputImpl() {
        this(false);
    }

    public TextOutputImpl(boolean compact) {
        this.compact = compact;
        this.out = new StringBuilder();
    }

    @Override
    public String toString() {
        return out.toString();
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public void indentIn() {
        ++identLevel;
        if (identLevel >= indents.length) {
            // Cache a new level of indentation string.
            char[] newIndentLevel = new char[identLevel * indentGranularity];
            Arrays.fill(newIndentLevel, ' ');
            char[][] newIndents = new char[indents.length + 1][];
            System.arraycopy(indents, 0, newIndents, 0, indents.length);
            newIndents[identLevel] = newIndentLevel;
            indents = newIndents;
        }
    }

    @Override
    public void indentOut() {
        --identLevel;
    }

    @Override
    public void newline() {
        out.append('\n');
        position++;
        line++;
        column = 0;
        justNewlined = true;
    }

    @Override
    public void newlineOpt() {
        if (!compact) {
            newline();
        }
    }

    @Override
    public void print(double value) {
        maybeIndent();
        int oldLength = out.length();
        out.append(value);
        movePosition(out.length() - oldLength);
    }

    @Override
    public void print(int value) {
        maybeIndent();
        int oldLength = out.length();
        out.append(value);
        movePosition(out.length() - oldLength);
    }

    @Override
    public void print(char c) {
        maybeIndent();
        out.append(c);
        movePosition(1);
    }

    private void movePosition(int l) {
        position += l;
        column += l;
        justNewlined = false;
    }

    @Override
    public void print(char[] s) {
        maybeIndent();
        printAndCount(s);
        justNewlined = false;
    }

    @Override
    public void print(CharSequence s) {
        maybeIndent();
        printAndCount(s);
        justNewlined = false;
    }

    @Override
    public void printOpt(char c) {
        if (!compact) {
            maybeIndent();
            out.append(c);
            position++;
            column++;
        }
    }

    @Override
    public void printOpt(char[] s) {
        if (!compact) {
            maybeIndent();
            printAndCount(s);
        }
    }

    @Override
    public void printOpt(String s) {
        if (!compact) {
            maybeIndent();
            printAndCount(s);
        }
    }

    private void maybeIndent() {
        if (justNewlined && !compact) {
            printAndCount(indents[identLevel]);
            justNewlined = false;
        }
    }

    private void printAndCount(CharSequence charSequence) {
        position += charSequence.length();
        column += charSequence.length();
        out.append(charSequence);
    }

    private void printAndCount(char[] chars) {
        position += chars.length;
        column += chars.length;
        out.append(chars);
    }
}
