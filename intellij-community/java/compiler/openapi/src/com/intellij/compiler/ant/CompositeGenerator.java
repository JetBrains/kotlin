/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.util.Pair;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * A composite generator
 *
 * @author Eugene Zhuravlev
 */
public class CompositeGenerator extends Generator {
  /**
   * child generators
   */
  private final List<Pair<Generator, Integer>> myGenerators = new ArrayList<>();
  /**
   * New line property
   */
  private boolean hasLeadingNewline = true;

  /**
   * A constructor
   */
  public CompositeGenerator() {
  }

  /**
   * A constructor that adds two elements
   *
   * @param generator1      the first generator
   * @param generator2      the second generator
   * @param emptyLinesCount the amount of new lines between two generators
   */
  public CompositeGenerator(Generator generator1, Generator generator2, int emptyLinesCount) {
    add(generator1);
    add(generator2, emptyLinesCount);
  }

  /**
   * By default, the composite generator generates an empty newline before generating nested eleemnts.
   * Setting the property to the false, allows suppressing it.
   *
   * @param value the new value of the property
   */
  public void setHasLeadingNewline(boolean value) {
    hasLeadingNewline = value;
  }

  /**
   * Add child generator with no emtpy lines before it
   *
   * @param generator the generator to add
   */
  public final void add(Generator generator) {
    add(generator, 0);
  }

  /**
   * Add child generator with the specified amount of empty lines before it
   *
   * @param generator       a generator
   * @param emptyLinesCount amount of empty lines
   */
  public final void add(Generator generator, int emptyLinesCount) {
    myGenerators.add(Pair.create(generator, new Integer(emptyLinesCount)));
  }

  /**
   * Generate content.
   *
   * @param out output stream
   * @throws IOException in case of IO propblem
   * @see #setHasLeadingNewline(boolean)
   */
  @Override
  public void generate(PrintWriter out) throws IOException {
    boolean first = true;
    for (final Pair<Generator, Integer> pair : myGenerators) {
      if (first) {
        if (hasLeadingNewline) {
          crlf(out);
        }
        first = false;
      }
      else {
        crlf(out);
      }
      final int emptyLinesCount = pair.getSecond().intValue();
      for (int idx = 0; idx < emptyLinesCount; idx++) {
        crlf(out);
      }
      pair.getFirst().generate(out);
    }
  }

  /**
   * @return amount of the child generators
   */
  public final int getGeneratorCount() {
    return myGenerators.size();
  }
}
