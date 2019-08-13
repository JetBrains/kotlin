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

package com.intellij.find.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.usages.TextChunk;
import com.intellij.usages.Usage;
import com.intellij.usages.UsagePresentation;
import com.intellij.usages.rules.UsageInFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class UsageListCellRenderer extends ColoredListCellRenderer {
  private final Project myProject;

  public UsageListCellRenderer(@NotNull Project project) {
    myProject = project;
  }

  @Override
  protected void customizeCellRenderer(@NotNull final JList list,
                                       final Object value,
                                       final int index,
                                       final boolean selected,
                                       final boolean hasFocus) {
    Usage usage = (Usage)value;
    UsagePresentation presentation = usage.getPresentation();
    setIcon(presentation.getIcon());
    VirtualFile virtualFile = getVirtualFile(usage);
    if (virtualFile != null) {
      append(virtualFile.getName() + ": ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
      setIcon(virtualFile.getFileType().getIcon());
      PsiFile psiFile = PsiManager.getInstance(myProject).findFile(virtualFile);
      if (psiFile != null) {
        setIcon(psiFile.getIcon(0));
      }
    }

    TextChunk[] text = presentation.getText();
    for (TextChunk textChunk : text) {
      SimpleTextAttributes simples = textChunk.getSimpleAttributesIgnoreBackground();
      append(textChunk.getText(), simples);
    }
  }

  public static VirtualFile getVirtualFile(final Usage usage) {
    return usage instanceof UsageInFile ? ((UsageInFile)usage).getFile() : null;
  }
}