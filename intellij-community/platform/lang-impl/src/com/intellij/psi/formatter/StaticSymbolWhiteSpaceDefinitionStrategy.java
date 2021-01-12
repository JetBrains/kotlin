/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.psi.formatter;

import gnu.trove.TIntHashSet;
import org.jetbrains.annotations.NotNull;

/**
 * {@link WhiteSpaceFormattingStrategy} implementation that is pre-configured with the set of symbols that may
 * be treated as white spaces.
 * <p/>
 * Please note that this class exists just for performance reasons (functionally we can use
 * {@link StaticTextWhiteSpaceDefinitionStrategy} with strings consisting from single symbol all the time).
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 */
public class StaticSymbolWhiteSpaceDefinitionStrategy extends AbstractWhiteSpaceFormattingStrategy {

  private final TIntHashSet myWhiteSpaceSymbols = new TIntHashSet();

  /**
   * Creates new {@code StaticWhiteSpaceDefinitionStrategy} object with the symbols that should be treated as white spaces.
   *
   * @param whiteSpaceSymbols   symbols that should be treated as white spaces by the current strategy
   */
  public StaticSymbolWhiteSpaceDefinitionStrategy(char ... whiteSpaceSymbols) {
    for (char symbol : whiteSpaceSymbols) {
      myWhiteSpaceSymbols.add(symbol);
    }
  }

  @Override
  public int check(@NotNull CharSequence text, int start, int end) {
    for (int i = start; i < end; i++) {
      char c = text.charAt(i);
      if (!myWhiteSpaceSymbols.contains(c)) {
        return i;
      }
    }
    return end;
  }
}
