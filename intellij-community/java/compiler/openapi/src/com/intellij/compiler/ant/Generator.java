/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.compiler.ant;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Eugene Zhuravlev
 */
public abstract class Generator {
  private static int ourIndent = 0;
  private static final int INDENT_SHIFT = 2;

  public abstract void generate(PrintWriter out) throws IOException;

  protected static void crlf(PrintWriter out) throws IOException {
    out.println();
    indent(out);
  }

  protected static void shiftIndent() {
    ourIndent += INDENT_SHIFT;
  }

  protected static void unshiftIndent() {
    ourIndent -= INDENT_SHIFT;
  }

  protected static void indent(PrintWriter out) throws IOException {
    for (int idx = 0; idx < ourIndent; idx++) {
      out.print(" ");
    }
  }

  /**
   * Write XML header
   * @param out a writer
   * @throws IOException in case of IO problem
   */
  protected static void writeXmlHeader(final PrintWriter out) throws IOException {
    //noinspection HardCodedStringLiteral
    out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    crlf(out);
  }

}
