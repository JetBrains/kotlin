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
import org.jetbrains.annotations.NotNull;

/**
 * Plain text bidi layout strategy: ignore any tokens, perform bidi layout on the whole line.
 */
public class PlainTextBidiRegionsSeparator extends BidiRegionsSeparator {
  @Override
  public boolean createBorderBetweenTokens(@NotNull IElementType previousTokenType, @NotNull IElementType tokenType) {
    return false;
  }
}
