/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.compiler.impl;

import com.intellij.compiler.ProblemsView;
import com.intellij.icons.AllIcons;
import com.intellij.ide.errorTreeView.ErrorTreeElement;
import com.intellij.ide.errorTreeView.ErrorTreeElementKind;
import com.intellij.ide.errorTreeView.ErrorViewStructure;
import com.intellij.ide.errorTreeView.GroupingElement;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * @author Eugene Zhuravlev
 */
public class ProblemsViewImpl extends ProblemsView{
  private static final String PROBLEMS_TOOLWINDOW_ID = "Problems";

  private final ProblemsViewPanel myPanel;
  private final ExecutorService myViewUpdater = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("ProblemsView Pool");
  private final Icon myActiveIcon = AllIcons.Toolwindows.Problems;
  private final Icon myPassiveIcon = AllIcons.Toolwindows.ProblemsEmpty;

  public ProblemsViewImpl(final Project project, final ToolWindowManager wm) {
    super(project);
    myPanel = new ProblemsViewPanel(project);
    Disposer.register(project, new Disposable() {
      @Override
      public void dispose() {
        Disposer.dispose(myPanel);
      }
    });
    UIUtil.invokeLaterIfNeeded(() -> {
      if (project.isDisposed()) {
        return;
      }
      final ToolWindow tw = wm.registerToolWindow(PROBLEMS_TOOLWINDOW_ID, false, ToolWindowAnchor.BOTTOM, project, true);
      final Content content = ContentFactory.SERVICE.getInstance().createContent(myPanel, "", false);
      content.setHelpId("reference.problems.tool.window");
      // todo: setup content?
      tw.getContentManager().addContent(content);
      Disposer.register(project, new Disposable() {
        @Override
        public void dispose() {
          tw.getContentManager().removeAllContents(true);
        }
      });
      updateIcon();
    });
  }

  @Override
  public void clearOldMessages(@Nullable final CompileScope scope, @NotNull final UUID currentSessionId) {
    myViewUpdater.execute(() -> {
      cleanupChildrenRecursively(myPanel.getErrorViewStructure().getRootElement(), scope, currentSessionId);
      updateIcon();
      myPanel.reload();
    });
  }

  private void cleanupChildrenRecursively(@NotNull final Object fromElement, final @Nullable CompileScope scope, @NotNull UUID currentSessionId) {
    final ErrorViewStructure structure = myPanel.getErrorViewStructure();
    for (ErrorTreeElement element : structure.getChildElements(fromElement)) {
      if (element instanceof GroupingElement) {
        if (scope != null) {
          final VirtualFile file = ((GroupingElement)element).getFile();
          if (file != null && !scope.belongs(file.getUrl())) {
            continue; 
          }
        }
        if (!currentSessionId.equals(element.getData())) {
          structure.removeElement(element);
        }
        else {
          cleanupChildrenRecursively(element, scope, currentSessionId);
        }
      }
      else {
        if (!currentSessionId.equals(element.getData())) {
          structure.removeElement(element);
        }
      }
    }
  }

  @Override
  public void addMessage(final int type,
                         @NotNull final String[] text,
                         @Nullable final String groupName,
                         @Nullable final Navigatable navigatable,
                         @Nullable final String exportTextPrefix, @Nullable final String rendererTextPrefix, @Nullable final UUID sessionId) {

    myViewUpdater.execute(() -> {
      final ErrorViewStructure structure = myPanel.getErrorViewStructure();
      final GroupingElement group = structure.lookupGroupingElement(groupName);
      if (group != null && sessionId != null && !sessionId.equals(group.getData())) {
        structure.removeElement(group);
      }
      if (navigatable != null) {
        myPanel.addMessage(type, text, groupName, navigatable, exportTextPrefix, rendererTextPrefix, sessionId);
      }
      else {
        myPanel.addMessage(type, text, null, -1, -1, sessionId);
      }
      updateIcon();
    });
  }

  private void updateIcon() {
    UIUtil.invokeLaterIfNeeded(() -> {
      if (!myProject.isDisposed()) {
        final ToolWindow tw = ToolWindowManager.getInstance(myProject).getToolWindow(PROBLEMS_TOOLWINDOW_ID);
        if (tw != null) {
          final boolean active = myPanel.getErrorViewStructure().hasMessages(EnumSet.of(ErrorTreeElementKind.ERROR, ErrorTreeElementKind.WARNING, ErrorTreeElementKind.NOTE));
          tw.setIcon(active ? myActiveIcon : myPassiveIcon);
        }
      }
    });
  }

  @Override
  public void setProgress(String text, float fraction) {
    myPanel.setProgress(text, fraction);
  }

  @Override
  public void setProgress(String text) {
    myPanel.setProgressText(text);
  }

  @Override
  public void clearProgress() {
    myPanel.clearProgressData();
  }
}
