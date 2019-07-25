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

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

abstract class AbstractIndentingBackspaceHandler extends BackspaceHandlerDelegate {
  private final SmartBackspaceMode myMode;
  private boolean myEnabled;

  AbstractIndentingBackspaceHandler(SmartBackspaceMode mode) {
    myMode = mode;
  }

  @Override
  public void beforeCharDeleted(char c, PsiFile file, Editor editor) {
    myEnabled = false;
    if (editor.isColumnMode() || !StringUtil.isWhiteSpace(c)) {
      return;
    }
    SmartBackspaceMode mode = getBackspaceMode(file.getLanguage());
    if (mode != myMode) {
      return;
    }
    doBeforeCharDeleted(c, file, editor);
    myEnabled = true;
  }

  @Override
  public boolean charDeleted(char c, PsiFile file, Editor editor) {
    if (!myEnabled) {
      return false;
    }
    return doCharDeleted(c, file, editor);
  }

  protected abstract void doBeforeCharDeleted(char c, PsiFile file, Editor editor);

  protected abstract boolean doCharDeleted(char c, PsiFile file, Editor editor);

  @NotNull
  private static SmartBackspaceMode getBackspaceMode(@NotNull Language language) {
    SmartBackspaceMode mode = CodeInsightSettings.getInstance().getBackspaceMode();
    BackspaceModeOverride override = LanguageBackspaceModeOverride.INSTANCE.forLanguage(language);
    if (override != null) {
      mode = override.getBackspaceMode(mode);
    }
    return mode;
  }
}
