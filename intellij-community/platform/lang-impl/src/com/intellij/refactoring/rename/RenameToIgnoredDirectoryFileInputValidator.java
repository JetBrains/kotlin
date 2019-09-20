/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.refactoring.rename;

import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.Nullable;

public class RenameToIgnoredDirectoryFileInputValidator implements RenameInputValidatorEx {
  @Nullable
  @Override
  public String getErrorMessage(String newName, Project project) {
    if (FileTypeManager.getInstance().isFileIgnored(newName)) {
      return "Trying to create a directory with ignored name, result will not be visible";
    }
    return null;
  }

  @Override
  public ElementPattern<? extends PsiElement> getPattern() {
    return PlatformPatterns.or(PlatformPatterns.psiElement(PsiDirectory.class), PlatformPatterns.psiElement(PsiFile.class));
  }

  @Override
  public boolean isInputValid(String newName, PsiElement element, ProcessingContext context) {
    return newName != null && newName.length() > 0 && newName.indexOf('\\') < 0 && newName.indexOf('/') < 0 && newName.indexOf('\n') < 0;
  }
}
