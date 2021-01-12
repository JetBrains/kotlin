/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

public class PsiFileRule implements GetDataRule {
  @Override
  public Object getData(@NotNull DataProvider dataProvider) {
    final PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataProvider);
    if (element != null) {
      return element.getContainingFile();
    }
    Project project = CommonDataKeys.PROJECT.getData(dataProvider);
    if (project != null) {
      VirtualFile vFile = CommonDataKeys.VIRTUAL_FILE.getData(dataProvider);
      if (vFile != null) {
        return PsiManager.getInstance(project).findFile(vFile);
      }
    }
    return null;
  }
}
