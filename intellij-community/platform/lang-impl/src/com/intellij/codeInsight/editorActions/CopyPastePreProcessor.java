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

package com.intellij.codeInsight.editorActions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RawText;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public interface CopyPastePreProcessor {
  ExtensionPointName<CopyPastePreProcessor> EP_NAME = ExtensionPointName.create("com.intellij.copyPastePreProcessor");

  /**
   * If not-null value is returned by this method, it will replace copied text. No other preprocessor will be invoked at copy time after this.
   */
  @Nullable
  String preprocessOnCopy(final PsiFile file, final int[] startOffsets, final int[] endOffsets, String text);

  /**
   * Replaces pasted text. {@code text} value should be returned if no processing is required.
   */
  @NotNull
  String preprocessOnPaste(final Project project, final PsiFile file, final Editor editor, String text, final RawText rawText);
}
