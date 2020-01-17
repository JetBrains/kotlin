// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.hierarchy.newAPI;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.LanguageCallHierarchy;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class CallHierarchyBrowserBase extends HierarchyBrowserBaseEx {
  /**
   * Use {code {@link #getCalleeType()}} instead
   */
  @Deprecated
  public static final String CALLEE_TYPE = "Callees of {0}";

  /**
   * Use {code {@link #getCallerType()}} instead
   */
  @Deprecated
  public static final String CALLER_TYPE = "Callers of {0}";

  @SuppressWarnings("UnresolvedPropertyKey") private static final HierarchyScopeType CALLEE =
    new HierarchyScopeType(() -> IdeBundle.message("title.hierarchy.callees.of"));

  @SuppressWarnings("UnresolvedPropertyKey") private static final HierarchyScopeType CALLER =
    new HierarchyScopeType(() -> IdeBundle.message("title.hierarchy.callers.of"));

  private static final String CALL_HIERARCHY_BROWSER_DATA_KEY = "com.intellij.ide.hierarchy.newAPI.CallHierarchyBrowserBase";

  public CallHierarchyBrowserBase(@NotNull Project project, @NotNull PsiElement method) {
    super(project, method);
  }

  @Override
  @Nullable
  protected JPanel createLegendPanel() {
    return null;
  }

  @Override
  @NotNull
  protected String getBrowserDataKey() {
    return CALL_HIERARCHY_BROWSER_DATA_KEY;
  }

  @Override
  protected void prependActions(@NotNull DefaultActionGroup actionGroup) {
    actionGroup.add(new ChangeViewTypeActionBase(IdeBundle.message("action.caller.methods.hierarchy"),
                                                 IdeBundle.message("action.caller.methods.hierarchy"),
                                                 AllIcons.Hierarchy.Supertypes, getCallerType()));
    actionGroup.add(new ChangeViewTypeActionBase(IdeBundle.message("action.callee.methods.hierarchy"),
                                                 IdeBundle.message("action.callee.methods.hierarchy"),
                                                 AllIcons.Hierarchy.Subtypes, getCalleeType()));
    actionGroup.add(new AlphaSortAction());
    actionGroup.add(new ChangeScopeAction());
  }

  @Override
  @NotNull
  protected String getActionPlace() {
    return ActionPlaces.CALL_HIERARCHY_VIEW_TOOLBAR;
  }

  @Override
  @NotNull
  protected String getPrevOccurenceActionNameImpl() {
    return IdeBundle.message("hierarchy.call.prev.occurence.name");
  }

  @Override
  @NotNull
  protected String getNextOccurenceActionNameImpl() {
    return IdeBundle.message("hierarchy.call.next.occurence.name");
  }

  private class ChangeViewTypeActionBase extends ToggleAction {
    private final HierarchyScopeType myTypeName;

    private ChangeViewTypeActionBase(final String shortDescription, final String longDescription, final Icon icon, HierarchyScopeType typeName) {
      super(shortDescription, longDescription, icon);
      myTypeName = typeName;
    }

    @Override
    public final boolean isSelected(@NotNull final AnActionEvent event) {
      return myTypeName == getCurrentViewType();
    }

    @Override
    public final void setSelected(@NotNull final AnActionEvent event, final boolean flag) {
      if (flag) {
        // invokeLater is called to update state of button before long tree building operation
        ApplicationManager.getApplication().invokeLater(() -> changeView(myTypeName));
      }
    }

    @Override
    public final void update(@NotNull final AnActionEvent event) {
      super.update(event);
      setEnabled(isValidBase());
    }
  }

  protected static class BaseOnThisMethodAction extends BaseOnThisElementAction {
    public BaseOnThisMethodAction() {
      super(IdeBundle.message("action.base.on.this.method"), CALL_HIERARCHY_BROWSER_DATA_KEY, LanguageCallHierarchy.INSTANCE);
    }
  }

  public static HierarchyScopeType getCalleeType() {
    return CALLEE;
  }

  public static HierarchyScopeType getCallerType() {
    return CALLER;
  }
}