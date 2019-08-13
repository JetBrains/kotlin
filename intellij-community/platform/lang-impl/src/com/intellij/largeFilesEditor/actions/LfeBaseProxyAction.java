// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.actions;

import com.intellij.largeFilesEditor.editor.EditorManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public abstract class LfeBaseProxyAction extends AnAction {

  private final AnAction originalAction;

  LfeBaseProxyAction(AnAction originalAction) {
    this.originalAction = originalAction;
    copyFrom(originalAction);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    EditorManager editorManager = Utils.tryGetLargeFileEditorManager(e);
    if (editorManager != null) {
      updateForLfe(e, editorManager);
    }
    else {
      originalAction.update(e);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    EditorManager editorManager = Utils.tryGetLargeFileEditorManager(e);
    if (editorManager != null) {
      actionPerformedForLfe(e, editorManager);
    }
    else {
      originalAction.actionPerformed(e);
    }
  }

  @Override
  public void beforeActionPerformedUpdate(@NotNull AnActionEvent e) {
    EditorManager editorManager = Utils.tryGetLargeFileEditorManager(e);
    if (editorManager != null) {
      updateForLfe(e, editorManager);
    }
    else {
      originalAction.beforeActionPerformedUpdate(e);
    }
  }

  @Override
  public boolean displayTextInToolbar() {
    return originalAction.displayTextInToolbar();
  }


  @Override
  public void setDefaultIcon(boolean isDefaultIconSet) {
    originalAction.setDefaultIcon(isDefaultIconSet);
  }

  @Override
  public boolean isDefaultIcon() {
    return originalAction.isDefaultIcon();
  }

  @Override
  public void setInjectedContext(boolean worksInInjected) {
    originalAction.setInjectedContext(worksInInjected);
  }

  @Override
  public boolean isInInjectedContext() {
    return originalAction.isInInjectedContext();
  }

  @Override
  public boolean isTransparentUpdate() {
    return originalAction.isTransparentUpdate();
  }

  @Override
  public boolean isDumbAware() {
    return originalAction.isDumbAware();
  }

  @Override
  public boolean startInTransaction() {
    return originalAction.startInTransaction();
  }

  @Override
  public String toString() {
    return originalAction.toString();
  }

  protected abstract void updateForLfe(AnActionEvent e, @NotNull EditorManager editorManager);

  protected abstract void actionPerformedForLfe(AnActionEvent e, @NotNull EditorManager editorManager);
}
