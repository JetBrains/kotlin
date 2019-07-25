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

package com.intellij.codeInsight.highlighting;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 *
 * @see HighlightUsagesHandlerFactoryBase
 */
public interface HighlightUsagesHandlerFactory {
  ExtensionPointName<HighlightUsagesHandlerFactory> EP_NAME = ExtensionPointName.create("com.intellij.highlightUsagesHandlerFactory");

  @Nullable
  HighlightUsagesHandlerBase createHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile file);


  /**
   * @param visibleRange To avoid parsing in EDT, these factory methods should be called in a background thread
   *                      (as implementation use the PSI element under cursor to choose the specific handler).
   *                      However, some handlers require the editor visible range, which must be calculated in EDT,
   *                      so it's passed externally
   */
  @Nullable
  default HighlightUsagesHandlerBase createHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile file, @NotNull ProperTextRange visibleRange) {
    return createHighlightUsagesHandler(editor, file);
  }
}
