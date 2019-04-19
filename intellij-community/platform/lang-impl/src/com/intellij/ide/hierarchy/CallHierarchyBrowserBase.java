/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.hierarchy;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class CallHierarchyBrowserBase extends HierarchyBrowserBaseEx {
  @SuppressWarnings("UnresolvedPropertyKey")
  public static final String CALLEE_TYPE = IdeBundle.message("title.hierarchy.callees.of");
  @SuppressWarnings("UnresolvedPropertyKey")
  public static final String CALLER_TYPE = IdeBundle.message("title.hierarchy.callers.of");

  private static final String CALL_HIERARCHY_BROWSER_DATA_KEY = "com.intellij.ide.hierarchy.CallHierarchyBrowserBase";

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
                                                 AllIcons.Hierarchy.Supertypes, CALLER_TYPE));
    actionGroup.add(new ChangeViewTypeActionBase(IdeBundle.message("action.callee.methods.hierarchy"),
                                                 IdeBundle.message("action.callee.methods.hierarchy"),
                                                 AllIcons.Hierarchy.Subtypes, CALLEE_TYPE));
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
    private final String myTypeName;

    private ChangeViewTypeActionBase(final String shortDescription, final String longDescription, final Icon icon, String typeName) {
      super(shortDescription, longDescription, icon);
      myTypeName = typeName;
    }

    @Override
    public final boolean isSelected(@NotNull final AnActionEvent event) {
      return myTypeName.equals(getCurrentViewType());
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
}