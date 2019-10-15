// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.content.*;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public abstract class DockablePopupManager<T extends JComponent & Disposable> {
  protected ToolWindow myToolWindow;
  private Runnable myAutoUpdateRequest;
  @NotNull protected final Project myProject;

  public DockablePopupManager(@NotNull Project project) {
    myProject = project;
  }

  protected abstract String getShowInToolWindowProperty();
  protected abstract String getAutoUpdateEnabledProperty();
  protected boolean getAutoUpdateDefault() {
    return false;
  }

  protected abstract String getAutoUpdateTitle();
  protected abstract String getRestorePopupDescription();
  protected abstract String getAutoUpdateDescription();

  protected abstract T createComponent();
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

  public void createToolWindow(@NotNull final PsiElement element, PsiElement originalElement) {
    assert myToolWindow == null;

    final T component = createComponent();

    final ToolWindowManagerEx toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(myProject);
    final ToolWindow toolWindow = toolWindowManagerEx.getToolWindow(getToolwindowId());
    myToolWindow = toolWindow == null
                   ? toolWindowManagerEx.registerToolWindow(getToolwindowId(), true, ToolWindowAnchor.RIGHT, myProject)
                   : toolWindow;
    myToolWindow.setIcon(AllIcons.Toolwindows.Documentation);

    myToolWindow.setAvailable(true, null);
    myToolWindow.setToHideOnEmptyContent(false);

    setToolwindowDefaultState();

    installComponentActions(myToolWindow, component);

    final ContentManager contentManager = myToolWindow.getContentManager();
    final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    final Content content = contentFactory.createContent(component, getTitle(element), false);
    contentManager.addContent(content);

    contentManager.addContentManagerListener(new ContentManagerAdapter() {
      @Override
      public void contentRemoved(@NotNull ContentManagerEvent event) {
        restorePopupBehavior();
      }
    });

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
    doUpdateComponent(element, originalElement, component);
  }

  protected void installComponentActions(ToolWindow toolWindow, T component) {
    ((ToolWindowEx)myToolWindow).setAdditionalGearActions(new DefaultActionGroup(createActions()));
  }

  protected void setToolwindowDefaultState() {
    final Rectangle rectangle = WindowManager.getInstance().getIdeFrame(myProject).suggestChildFrameBounds();
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
    return new DumbAwareAction("Open as Popup", getRestorePopupDescription(), null) {
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

  public void updateComponent() {
    updateComponent(false);
  }

  public void updateComponent(boolean requestFocus) {
    if (myProject.isDisposed()) return;

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
      if (editor.isDisposed()) return;

      PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, myProject);
      Editor injectedEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file);
      PsiFile injectedFile = injectedEditor != null ? PsiUtilBase.getPsiFileInEditor(injectedEditor, myProject) : null;
      if (injectedFile != null) {
        doUpdateComponent(injectedEditor, injectedFile, requestFocus);
      }
      else if (file != null) {
        doUpdateComponent(editor, file, requestFocus);
      }
    });
  }


  public void restorePopupBehavior() {
    if (myToolWindow != null) {
      PropertiesComponent.getInstance().setValue(getShowInToolWindowProperty(), Boolean.FALSE.toString());
      ToolWindowManagerEx toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(myProject);
      toolWindowManagerEx.hideToolWindow(getToolwindowId(), false);
      toolWindowManagerEx.unregisterToolWindow(getToolwindowId());
      Disposer.dispose(myToolWindow.getContentManager());
      myToolWindow = null;
      restartAutoUpdate(false);
    }
  }

  public boolean hasActiveDockedDocWindow() {
    return myToolWindow != null && myToolWindow.isVisible();
  }
}
