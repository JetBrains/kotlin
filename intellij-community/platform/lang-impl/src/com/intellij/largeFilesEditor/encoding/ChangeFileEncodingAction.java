// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.encoding;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.nio.charset.Charset;

public class ChangeFileEncodingAction extends com.intellij.openapi.vfs.encoding.ChangeFileEncodingAction {

  private static final Logger logger = Logger.getInstance(ChangeFileEncodingAction.class);

  private final EditorManagerAccessor editorManagerAccessor;
  private final Project project;
  private final StatusBar statusBar;

  ChangeFileEncodingAction(EditorManagerAccessor editorManagerAccessor, Project project, StatusBar statusBar) {
    this.editorManagerAccessor = editorManagerAccessor;
    this.project = project;
    this.statusBar = statusBar;
  }

  private boolean chosen(@NotNull Charset charset) {
    EditorManagerAccess editorManagerAccess = editorManagerAccessor.getAccess(project, statusBar);
    if (editorManagerAccess == null) {
      logger.warn("tried to change encoding while editor is not accessible");
      return false;
    }
    boolean result = editorManagerAccess.tryChangeEncoding(charset);
    updateWidget();
    return result;
  }

  private void updateWidget() {
    StatusBarWidget widget = statusBar.getWidget(EncodingWidget.WIDGET_ID);
    if (widget instanceof EncodingWidget) {
      ((EncodingWidget)widget).requestUpdate();
    }
    else {
      logger.warn("[LargeFileEditorSubsystem] ChangeFileEncodingAction.updateWidget(): "
                  + (widget == null
                     ? " variable 'widget' is null"
                     : " variable is instance of " + widget.getClass().getName()));
    }
  }

  @Override
  protected boolean chosen(Document document, Editor editor, @Nullable VirtualFile virtualFile, byte[] bytes, @NotNull Charset charset) {
    return chosen(charset);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(true);
    e.getPresentation().setVisible(true);
  }

  ListPopup createPopup(VirtualFile vFile, Editor editor, Component componentParent) {
    DataContext dataContext = wrapInDataContext(vFile, editor, componentParent);
    DefaultActionGroup group = createActionGroup(vFile, editor, null, null, null);
    return JBPopupFactory.getInstance().createActionGroupPopup(getTemplatePresentation().getText(),
                                                               group, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
  }

  private static DataContext wrapInDataContext(VirtualFile vFile, Editor editor, Component componentParent) {
    DataContext parent = DataManager.getInstance().getDataContext(componentParent);
    return SimpleDataContext.getSimpleContext(
      ContainerUtil.<String, Object>immutableMapBuilder()
        .put(CommonDataKeys.VIRTUAL_FILE.getName(), vFile)
        .put(CommonDataKeys.VIRTUAL_FILE_ARRAY.getName(), new VirtualFile[]{vFile})
        .put(CommonDataKeys.PROJECT.getName(), editor.getProject())
        .put(PlatformDataKeys.CONTEXT_COMPONENT.getName(), editor.getComponent())
        .build(),
      parent);
  }
}
