/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.editor.actions;

import com.intellij.codeInsight.completion.NextPrevParameterAction;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;

/**
 * @author peter
 */
public class LangIndentSelectionAction extends IndentSelectionAction {

  @Override
  protected boolean isEnabled(Editor editor, DataContext dataContext) {
    if (!originalIsEnabled(editor, wantSelection())) return false;
    if (LookupManager.getActiveLookup(editor) != null) return false;

    PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataContext);
    if (psiFile != null && NextPrevParameterAction.hasSuitablePolicy(editor, psiFile)) return false;

    return true;
  }

  protected boolean wantSelection() {
    return true;
  }
}
