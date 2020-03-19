// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.content.*;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class DockablePopupManager<T extends JComponent & Disposable> {
  private final static Logger LOG = Logger.getInstance(DockablePopupManager.class);
  protected ToolWindow myToolWindow;
  private Runnable myAutoUpdateRequest;
  @NotNull protected final Project myProject;

  public DockablePopupManager(@NotNull Project project) {
    myProject = project;
  }

  @Nls
  protected abstract String getShowInToolWindowProperty();

  @Nls
  protected abstract String getAutoUpdateEnabledProperty();

  protected boolean getAutoUpdateDefault() {
    return false;
  }

  @Nls
  protected abstract String getAutoUpdateTitle();

  @Nls
  protected abstract String getRestorePopupDescription();

  @Nls
  protected abstract String getAutoUpdateDescription();

  protected abstract T createComponent();

  protected void doUpdateComponent(@NotNull CompletableFuture<PsiElement> elementFuture, PsiElement originalElement, T component) {
    try {
      doUpdateComponent(elementFuture.get(), originalElement, component);
    }
    catch (InterruptedException | ExecutionException e) {
      LOG.debug("Cannot update component", e);
    }
  }

  protected abstract void doUpdateComponent(@NotNull PsiElement element, PsiElement originalElement, T component);

  protected void doUpdateComponent(Editor editor, PsiFile psiFile, boolean requestFocus) { doUpdateComponent(editor, psiFile); }

  protected abstract void doUpdateComponent(Editor editor, PsiFile psiFile);

  protected abstract void doUpdateComponent(@NotNull PsiElement element);

  protected abstract String getTitle(PsiElement element);

  protected abstract String getToolwindowId();

  public Content recreateToolWindow(PsiElement element, PsiElement originalElement) {
    if (myToolWindow == null) {
      createToolWindow(element, originalElement);
      return null;
    }

    final Content content = myToolWindow.getContentManager().getSelectedContent();
    if (content == null || !myToolWindow.isVisible()) {
      restorePopupBehavior();
      createToolWindow(element, originalElement);
      return null;
    }
    return content;
  }

  public void createToolWindow(@NotNull PsiElement element, PsiElement originalElement) {
    doCreateToolWindow(element, null, originalElement);
  }

  public void createToolWindow(@NotNull CompletableFuture<PsiElement> elementFuture, PsiElement originalElement) {
    doCreateToolWindow(null, elementFuture, originalElement);
  }

  private void doCreateToolWindow(@Nullable PsiElement element,
                                  @Nullable CompletableFuture<PsiElement> elementFuture,
                                  PsiElement originalElement) {
    assert myToolWindow == null;

    T component = createComponent();

    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(getToolwindowId());
    if (toolWindow == null) {
      toolWindow = toolWindowManager
        .registerToolWindow(RegisterToolWindowTask.closable(getToolwindowId(), AllIcons.Toolwindows.Documentation, ToolWindowAnchor.RIGHT));
    }
    else {
      toolWindow.setAvailable(true);
    }
    myToolWindow = toolWindow;

    toolWindow.setToHideOnEmptyContent(false);

    setToolwindowDefaultState();

    ContentManager contentManager = toolWindow.getContentManager();
    String displayName = element != null ? getTitle(element) : "";
    contentManager.addContent(ContentFactory.SERVICE.getInstance().createContent(component, displayName, false));
    contentManager.addContentManagerListener(new ContentManagerListener() {
      @Override
      public void contentRemoved(@NotNull ContentManagerEvent event) {
        restorePopupBehavior();
      }
    });

    installComponentActions(toolWindow, component);

    new UiNotifyConnector(component, new Activatable() {
      @Override
      public void showNotify() {
        restartAutoUpdate(PropertiesComponent.getInstance().getBoolean(getAutoUpdateEnabledProperty(), getAutoUpdateDefault()));
      }

      @Override
      public void hideNotify() {
        restartAutoUpdate(false);
      }
    });

    myToolWindow.show(null);
    PropertiesComponent.getInstance().setValue(getShowInToolWindowProperty(), Boolean.TRUE.toString());
    restartAutoUpdate(PropertiesComponent.getInstance().getBoolean(getAutoUpdateEnabledProperty(), true));
    if (element != null) {
      doUpdateComponent(element, originalElement, component);
    }
    else {
      //noinspection ConstantConditions
      doUpdateComponent(elementFuture, originalElement, component);
    }
  }

  protected void installComponentActions(@NotNull ToolWindow toolWindow, T component) {
    ((ToolWindowEx)myToolWindow).setAdditionalGearActions(new DefaultActionGroup(createActions()));
  }

  protected void setToolwindowDefaultState() {
    Rectangle rectangle = WindowManager.getInstance().getIdeFrame(myProject).suggestChildFrameBounds();
    myToolWindow.setDefaultState(ToolWindowAnchor.RIGHT, ToolWindowType.FLOATING, rectangle);
  }

  protected AnAction[] createActions() {
    ToggleAction toggleAutoUpdateAction = new ToggleAction(getAutoUpdateTitle(), getAutoUpdateDescription(),
                                           AllIcons.General.AutoscrollFromSource) {
      @Override
      public boolean isSelected(@NotNull AnActionEvent e) {
        return PropertiesComponent.getInstance().getBoolean(getAutoUpdateEnabledProperty(),
                                                            getAutoUpdateDefault());
      }

      @Override
      public void setSelected(@NotNull AnActionEvent e, boolean state) {
        PropertiesComponent.getInstance().setValue(getAutoUpdateEnabledProperty(), state, getAutoUpdateDefault());
        restartAutoUpdate(state);
      }
    };
    return new AnAction[]{createRestorePopupAction(), toggleAutoUpdateAction};
  }

  @NotNull
  protected AnAction createRestorePopupAction() {
    return new DumbAwareAction(CodeInsightBundle.messagePointer("action.AnActionButton.text.open.as.popup"), () -> getRestorePopupDescription(), null) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        restorePopupBehavior();
      }
    };
  }

  void restartAutoUpdate(final boolean state) {
    if (state && myToolWindow != null) {
      if (myAutoUpdateRequest == null) {
        myAutoUpdateRequest = this::updateComponent;

        UIUtil.invokeLaterIfNeeded(() -> IdeEventQueue.getInstance().addIdleListener(myAutoUpdateRequest, 500));
      }
    }
    else {
      if (myAutoUpdateRequest != null) {
        IdeEventQueue.getInstance().removeIdleListener(myAutoUpdateRequest);
        myAutoUpdateRequest = null;
      }
    }
  }

  public void resetAutoUpdateState() {
    restartAutoUpdate(PropertiesComponent.getInstance().getBoolean(getAutoUpdateEnabledProperty(), getAutoUpdateDefault()));
  }

  public void updateComponent() {
    updateComponent(false);
  }

  public void updateComponent(boolean requestFocus) {
    if (myProject.isDisposed()) {
      return;
    }

    DataManager.getInstance()
      .getDataContextFromFocusAsync()
      .onSuccess(dataContext -> {
        if (!myProject.isOpen()) return;
        updateComponentInner(dataContext, requestFocus);
      });
  }

  private void updateComponentInner(@NotNull DataContext dataContext, boolean requestFocus) {
    if (CommonDataKeys.PROJECT.getData(dataContext) != myProject) {
      return;
    }

    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor == null) {
      PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
      if (element != null) {
        doUpdateComponent(element);
      }
      return;
    }

    PsiDocumentManager.getInstance(myProject).performLaterWhenAllCommitted(() -> {
      if (editor.isDisposed()) {
        return;
      }

      PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, myProject);
      Editor injectedEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file);
      PsiFile injectedFile = PsiUtilBase.getPsiFileInEditor(injectedEditor, myProject);
      if (injectedFile != null) {
        doUpdateComponent(injectedEditor, injectedFile, requestFocus);
      }
      else if (file != null) {
        doUpdateComponent(editor, file, requestFocus);
      }
    });
  }


  public void restorePopupBehavior() {
    ToolWindow toolWindow = myToolWindow;
    if (toolWindow == null) {
      return;
    }

    PropertiesComponent.getInstance().setValue(getShowInToolWindowProperty(), Boolean.FALSE.toString());
    toolWindow.remove();
    Disposer.dispose(toolWindow.getContentManager());
    myToolWindow = null;
    restartAutoUpdate(false);
  }

  public boolean hasActiveDockedDocWindow() {
    return myToolWindow != null && myToolWindow.isVisible();
  }
}
