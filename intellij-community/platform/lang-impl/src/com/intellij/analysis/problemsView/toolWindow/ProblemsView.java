// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.ide.actions.ToggleToolbarAction.isToolbarVisible;
import static com.intellij.psi.util.PsiUtilCore.findFileSystemItem;

public final class ProblemsView implements DumbAware, ToolWindowFactory {
  private static final String ID = "Problems View";
  private static final int CURRENT_FILE_INDEX = 0;

  public static @Nullable ToolWindow getToolWindow(@Nullable Project project) {
    return project == null || project.isDisposed() ? null : ToolWindowManager.getInstance(project).getToolWindow(ID);
  }

  public static void toggleCurrentFileProblems(@NotNull Project project) {
    ToolWindow window = getToolWindow(project);
    if (window == null) return; // does not exist
    ContentManager manager = window.getContentManager();
    if (window.isVisible() && manager.getSelectedContent() == manager.getContent(CURRENT_FILE_INDEX)) {
      window.hide(); // hide toolwindow only if the Current File tab is selected
    }
    else {
      selectContent(manager, CURRENT_FILE_INDEX);
      window.setAvailable(true, null);
      window.activate(null, true);
    }
  }

  public static void selectHighlighterIfVisible(@NotNull Project project, @NotNull RangeHighlighterEx highlighter) {
    HighlightingPanel panel = get(HighlightingPanel.class, getSelectedContent(project));
    if (panel != null && panel.isShowing()) panel.selectHighlighter(highlighter);
  }

  static @Nullable Document getDocument(@Nullable Project project, @NotNull VirtualFile file) {
    Object item = file.isDirectory() ? null : findFileSystemItem(project, file);
    return item instanceof PsiFile ? PsiDocumentManager.getInstance(project).getDocument((PsiFile)item) : null;
  }

  static @Nullable ProblemsViewPanel getSelectedPanel(@Nullable Project project) {
    return get(ProblemsViewPanel.class, getSelectedContent(project));
  }

  private static @Nullable Content getSelectedContent(@Nullable Project project) {
    ToolWindow window = getToolWindow(project);
    ContentManager manager = window == null ? null : window.getContentManagerIfCreated();
    return manager == null ? null : manager.getSelectedContent();
  }

  private static void createContent(@NotNull ContentManager manager, @NotNull ProblemsViewPanel panel) {
    Content content = manager.getFactory().createContent(panel, panel.getDisplayName(), false);
    content.setCloseable(false);
    manager.addContent(content);
  }

  private static void selectContent(@NotNull ContentManager manager, int index) {
    Content content = manager.getContent(index);
    if (content != null) manager.setSelectedContent(content);
  }

  private static void selectionChanged(boolean selected, @Nullable Content content) {
    ProblemsViewPanel panel = get(ProblemsViewPanel.class, content);
    if (panel != null) panel.selectionChangedTo(selected);
  }

  @SuppressWarnings("unchecked")
  private static <T> @Nullable T get(@NotNull Class<T> type, @Nullable Content content) {
    JComponent component = content == null ? null : content.getComponent();
    return type.isInstance(component) ? (T)component : null;
  }

  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    return Experiments.getInstance().isFeatureEnabled("problems.view.enabled");
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow window) {
    ProblemsViewState state = ProblemsViewState.getInstance(project);
    state.setShowToolbar(isToolbarVisible(window, PropertiesComponent.getInstance(project)));
    ContentManager manager = window.getContentManager();
    createContent(manager, new HighlightingPanel(project, state));
    if (Experiments.getInstance().isFeatureEnabled("problems.view.project.errors.enabled"))
    createContent(manager, new ProjectErrorsPanel(project, state));
    selectContent(manager, state.getSelectedIndex());
    selectionChanged(true, manager.getSelectedContent());
    manager.addContentManagerListener(new ContentManagerListener() {
      @Override
      public void selectionChanged(@NotNull ContentManagerEvent event) {
        boolean selected = ContentManagerEvent.ContentOperation.add == event.getOperation();
        ProblemsView.selectionChanged(selected, event.getContent());
        if (selected) state.setSelectedIndex(event.getIndex());
      }
    });
    project.getMessageBus().connect(manager).subscribe(ToolWindowManagerListener.TOPIC, createListener());
  }

  @NotNull
  private static ToolWindowManagerListener createListener() {
    return new ToolWindowManagerListener() {
      private final AtomicBoolean orientation = new AtomicBoolean();

      @Override
      public void stateChanged(@NotNull ToolWindowManager manager) {
        ToolWindow window = manager.getToolWindow(ID);
        if (window == null || window.isDisposed()) return;

        boolean vertical = !window.getAnchor().isHorizontal();
        if (vertical != orientation.getAndSet(vertical)) {
          for (Content content : window.getContentManager().getContents()) {
            ProblemsViewPanel panel = get(ProblemsViewPanel.class, content);
            if (panel != null) panel.orientationChangedTo(vertical);
          }
        }
      }
    };
  }
}
