/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.editor.bidi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link BidiRegionsSeparator} allowing to define a number of token sets, tokens within each set will be processed 
 * as a whole by bidi layout algorithm.
 */
public abstract class TokenSetBidiRegionsSeparator extends BidiRegionsSeparator {
  private final TokenSet[] myTokenSets;

  protected TokenSetBidiRegionsSeparator(TokenSet... tokenSets) {
    myTokenSets = tokenSets;
  }
  
  @Override
  public boolean createBorderBetweenTokens(@NotNull IElementType previousTokenType, @NotNull IElementType tokenType) {
    for (TokenSet set : myTokenSets) {
      if (set.contains(previousTokenType) && set.contains(tokenType)) {
        return false;
      }
    }
    return true;
  }
}
