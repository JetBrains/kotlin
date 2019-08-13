/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInsight.hint;

import com.intellij.openapi.util.MixinExtension;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeclarationRangeUtil {
  @NotNull
  public static TextRange getDeclarationRange(@NotNull PsiElement container) {
    TextRange textRange = getPossibleDeclarationAtRange(container);
    assert textRange != null : "Declaration range is invalid for " + container.getClass();
    return textRange;
  }

  @Nullable
  public static TextRange getPossibleDeclarationAtRange(@NotNull PsiElement container) {
    DeclarationRangeHandler handler = MixinExtension.getInstance(DeclarationRangeHandler.EP_NAME, container);
    //noinspection unchecked
    return handler != null ? handler.getDeclarationRange(container) : null;
  }
}