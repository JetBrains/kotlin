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
package com.intellij.ide.actions;

import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class DefaultSearchEverywhereClassifier implements SearchEverywhereClassifier {
  @Override
  public boolean isClass(@Nullable Object o) {
    return o instanceof PsiElement;
  }

  @Override
  public boolean isSymbol(@Nullable Object o) {
    if (o instanceof PsiElement) {
      final PsiElement e = (PsiElement)o;
      return !e.getLanguage().is(Language.findLanguageByID("JAVA")) || !(e.getParent() instanceof PsiFile);
    }
    return false;
  }

  @Nullable
  @Override
  public VirtualFile getVirtualFile(@NotNull Object o) {
    if (o instanceof PsiElement) {
      final PsiElement element = (PsiElement)o;
      final PsiFile file = element.getContainingFile();
      return file != null ? file.getVirtualFile() : null;
    }
    return null;
  }

  @Nullable
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    return null;
  }
}
