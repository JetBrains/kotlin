// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.copy;

import com.intellij.ide.TwoPaneIdeView;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.structureView.StructureViewFactoryEx;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class CopyHandler {
  private CopyHandler() {
  }

  public static boolean canCopy(PsiElement[] elements) {
    return canCopy(elements, null);
  }

  public static boolean canCopy(PsiElement[] elements, @Nullable Ref<String> actionName) {
    if (elements.length > 0) {
      for(CopyHandlerDelegate delegate: CopyHandlerDelegate.EP_NAME.getExtensionList()) {
        if (delegate instanceof CopyHandlerDelegateBase ? ((CopyHandlerDelegateBase)delegate).canCopy(elements, true) : delegate.canCopy(elements)) {
          if (actionName != null) {
            actionName.set(delegate.getActionName(elements));
          }
          return true;
        }
      }
    }
    return false;
  }


  public static void doCopy(PsiElement[] elements, PsiDirectory defaultTargetDirectory) {
    if (elements.length == 0) return;
    for(CopyHandlerDelegate delegate: CopyHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canCopy(elements)) {
        delegate.doCopy(elements, defaultTargetDirectory);
        break;
      }
    }
  }

  public static boolean canClone(PsiElement[] elements) {
    if (elements.length > 0) {
      for (CopyHandlerDelegate delegate : CopyHandlerDelegate.EP_NAME.getExtensionList()) {
        if (delegate instanceof CopyHandlerDelegateBase ? ((CopyHandlerDelegateBase)delegate).canCopy(elements, true) : delegate.canCopy(elements)) {
          if (delegate instanceof CopyHandlerDelegateBase && ((CopyHandlerDelegateBase)delegate).forbidToClone(elements, true)){
            return false;
          }
          return true;
        }
      }
    }
    return false;
  }

  public static void doClone(PsiElement element) {
    PsiElement[] elements = new PsiElement[]{element};
    for(CopyHandlerDelegate delegate: CopyHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canCopy(elements)) {
        if (delegate instanceof CopyHandlerDelegateBase && ((CopyHandlerDelegateBase)delegate).forbidToClone(elements, false)) {
          return;
        }
        delegate.doClone(element);
        break;
      }
    }
  }

  public static void updateSelectionInActiveProjectView(@NotNull PsiElement newElement, Project project, boolean selectInActivePanel) {
    String id = ToolWindowManager.getInstance(project).getActiveToolWindowId();
    if (id != null) {
      ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(id);
      Content selectedContent = window.getContentManager().getSelectedContent();
      if (selectedContent != null) {
        JComponent component = selectedContent.getComponent();
        if (component instanceof TwoPaneIdeView) {
          ((TwoPaneIdeView) component).selectElement(newElement, selectInActivePanel);
          return;
        }
      }
    }
    if (ToolWindowId.PROJECT_VIEW.equals(id)) {
      ProjectView.getInstance(project).selectPsiElement(newElement, true);
    }
    else if (ToolWindowId.STRUCTURE_VIEW.equals(id)) {
      VirtualFile virtualFile = newElement.getContainingFile().getVirtualFile();
      FileEditor editor = FileEditorManager.getInstance(newElement.getProject()).getSelectedEditor(virtualFile);
      StructureViewFactoryEx.getInstanceEx(project).getStructureViewWrapper().selectCurrentElement(editor, virtualFile, true);
    }
  }
}
