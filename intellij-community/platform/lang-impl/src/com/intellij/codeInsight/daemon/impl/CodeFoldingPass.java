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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.EditorBoundHighlightingPass;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.codeInsight.folding.impl.FoldingUpdate;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.PossiblyDumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

class CodeFoldingPass extends EditorBoundHighlightingPass implements PossiblyDumbAware {
  private static final Key<Boolean> THE_FIRST_TIME = Key.create("FirstFoldingPass");
  private volatile Runnable myRunnable;

  CodeFoldingPass(@NotNull Editor editor, @NotNull PsiFile file) {
    super(editor, file, false);
  }

  @Override
  public void doCollectInformation(@NotNull ProgressIndicator progress) {
    final boolean firstTime = isFirstTime(myFile, myEditor, THE_FIRST_TIME);
    myRunnable = CodeFoldingManager.getInstance(myProject).updateFoldRegionsAsync(myEditor, firstTime);
  }

  static boolean isFirstTime(PsiFile file, Editor editor, Key<Boolean> key) {
    return file.getUserData(key) == null || editor.getUserData(key) == null;
  }

  static void clearFirstTimeFlag(PsiFile file, Editor editor, Key<? super Boolean> key) {
    file.putUserData(key, Boolean.FALSE);
    editor.putUserData(key, Boolean.FALSE);
  }

  @Override
  public void doApplyInformationToEditor() {
    Runnable runnable = myRunnable;
    if (runnable != null){
      try {
        runnable.run();
      }
      catch (IndexNotReadyException ignored) {
      }
    }

    if (InjectedLanguageManager.getInstance(myFile.getProject()).getTopLevelFile(myFile) == myFile) {
      clearFirstTimeFlag(myFile, myEditor, THE_FIRST_TIME);
    }
  }

  /**
   * Checks the ability to update folding in the Dumb Mode. True by default.
   * @return true if the language implementation can update folding ranges
   */
  @Override
  public boolean isDumbAware() {
    return FoldingUpdate.supportsDumbModeFolding(myEditor);
  }
}
