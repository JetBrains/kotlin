/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.impl.dataRules;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

public class VirtualFileRule implements GetDataRule {
  @Override
  public Object getData(@NotNull final DataProvider dataProvider) {
    // Try to detect multi-selection.
    PsiElement[] psiElements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(dataProvider);
    if (psiElements != null) {
      for (PsiElement elem : psiElements) {
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(elem);
        if (virtualFile != null) return virtualFile;
      }
    }

    VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataProvider);
    if (virtualFiles != null && virtualFiles.length == 1) {
      return virtualFiles[0];
    }

    PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataProvider);
    if (psiFile != null) {
      return psiFile.getVirtualFile();
    }
    PsiElement elem = CommonDataKeys.PSI_ELEMENT.getData(dataProvider);
    if (elem == null) {
      return null;
    }
    return PsiUtilCore.getVirtualFile(elem);
  }
}
