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
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Dmitry Batkovich
 */
public class InspectionResultsViewUtil {
  static void releaseEditor(@Nullable Editor editor) {
    if (editor != null && !editor.isDisposed()) {
      EditorFactory.getInstance().releaseEditor(editor);
    }
  }

  @Nullable
  static Navigatable getNavigatableForInvalidNode(ProblemDescriptionNode node) {
    RefEntity element = node.getElement();
    while (element != null && !element.isValid()) {
      element = element.getOwner();
    }
    if (!(element instanceof RefElement)) return null;
    PsiElement containingElement = ((RefElement)element).getPsiElement();
    if (!(containingElement instanceof NavigatablePsiElement) || !containingElement.isValid()) return null;

    final int lineNumber = node.getLineNumber();
    if (lineNumber != -1) {
      final PsiFile containingFile = containingElement.getContainingFile();
      if (containingFile != null) {
        final VirtualFile file = containingFile.getVirtualFile();
        final Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null && document.getLineCount() > lineNumber) {
          return new OpenFileDescriptor(containingElement.getProject(), file, lineNumber, 0);
        }
      }
    }
    return (Navigatable)containingElement;
  }

  @NotNull
  static JLabel getNothingToShowTextLabel() {
    return createLabelForText(InspectionViewNavigationPanel.getTitleText(false));
  }

  @NotNull
  static JComponent getInvalidEntityLabel(@NotNull RefEntity entity) {
    final String name = entity.getName();
    return createLabelForText("\'" + name + "\' is no longer valid.");
  }

  public static JComponent getPreviewIsNotAvailable(@NotNull RefEntity entity) {
    final String name = entity.getQualifiedName();
    return createLabelForText("Preview is not available for \'" + name + "\'.");
  }

  @NotNull
  static JComponent getApplyingFixLabel(@NotNull InspectionToolWrapper wrapper) {
    return createLabelForText("Applying quick fix for \'" + wrapper.getDisplayName() + "\'...");
  }

  @NotNull
  static JLabel createLabelForText(String text) {
    final JLabel multipleSelectionLabel = new JBLabel(text);
    multipleSelectionLabel.setVerticalAlignment(SwingConstants.TOP);
    multipleSelectionLabel.setBorder(JBUI.Borders.empty(16, 12, 0, 0));
    return multipleSelectionLabel;
  }
}
