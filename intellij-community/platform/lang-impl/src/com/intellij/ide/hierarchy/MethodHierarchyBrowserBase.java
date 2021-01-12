/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.intellij.openapi.ui.MultiLineLabelUI;
import com.intellij.psi.PsiElement;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class MethodHierarchyBrowserBase extends HierarchyBrowserBaseEx {
  public static final String METHOD_TYPE = "Method {0}";

  public static final DataKey<MethodHierarchyBrowserBase> DATA_KEY = DataKey.create("com.intellij.ide.hierarchy.MethodHierarchyBrowserBase");

  public MethodHierarchyBrowserBase(final Project project, final PsiElement method) {
    super(project, method);
  }

  @Override
  @NotNull
  protected String getPrevOccurenceActionNameImpl() {
    return IdeBundle.message("hierarchy.method.prev.occurence.name");
  }

  @Override
  protected Map<String, Supplier<String>> getPresentableNameMap() {
    HashMap<String, Supplier<String>> map = new HashMap<>();
    map.put(METHOD_TYPE, MethodHierarchyBrowserBase::getMethodType);
    return map;
  }

  @Override
  @NotNull
  protected String getNextOccurenceActionNameImpl() {
    return IdeBundle.message("hierarchy.method.next.occurence.name");
  }

  protected static JPanel createStandardLegendPanel(final String methodDefinedText,
                                                    final String methodNotDefinedLegallyText,
                                                    final String methodShouldBeDefined) {
    final JPanel panel = new JPanel(new GridBagLayout());

    final GridBagConstraints gc =
      new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(3, 5, 0, 5), 0, 0);

    JLabel label = new JLabel(methodDefinedText, AllIcons.Hierarchy.MethodDefined, SwingConstants.LEFT);
    label.setUI(new MultiLineLabelUI());
    label.setIconTextGap(10);
    panel.add(label, gc);

    gc.gridy++;
    label = new JLabel(methodNotDefinedLegallyText, AllIcons.Hierarchy.MethodNotDefined, SwingConstants.LEFT);
    label.setUI(new MultiLineLabelUI());
    label.setIconTextGap(10);
    panel.add(label, gc);

    gc.gridy++;
    label = new JLabel(methodShouldBeDefined, AllIcons.Hierarchy.ShouldDefineMethod, SwingConstants.LEFT);
    label.setUI(new MultiLineLabelUI());
    label.setIconTextGap(10);
    panel.add(label, gc);

    return panel;
  }

  @Override
  protected void prependActions(@NotNull DefaultActionGroup actionGroup) {
    actionGroup.add(new AlphaSortAction());
    actionGroup.add(new ShowImplementationsOnlyAction());
    actionGroup.add(new ChangeScopeAction());
  }

  @Override
  @NotNull
  protected String getBrowserDataKey() {
    return DATA_KEY.getName();
  }

  @Override
  @NotNull
  protected String getActionPlace() {
    return ActionPlaces.METHOD_HIERARCHY_VIEW_TOOLBAR;
  }

  private final class ShowImplementationsOnlyAction extends ToggleAction {
    private ShowImplementationsOnlyAction() {
      super(IdeBundle.messagePointer("action.hide.non.implementations"), AllIcons.General.Filter);
    }

    @Override
    public final boolean isSelected(@NotNull final AnActionEvent event) {
      return HierarchyBrowserManager.getInstance(myProject).getState().HIDE_CLASSES_WHERE_METHOD_NOT_IMPLEMENTED;
    }

    @Override
    public final void setSelected(@NotNull final AnActionEvent event, final boolean flag) {
      HierarchyBrowserManager.getInstance(myProject).getState().HIDE_CLASSES_WHERE_METHOD_NOT_IMPLEMENTED = flag;
      // invokeLater is called to update state of button before long tree building operation
      ApplicationManager.getApplication().invokeLater(() -> doRefresh(true));
    }

    @Override
    public final void update(@NotNull final AnActionEvent event) {
      super.update(event);
      final Presentation presentation = event.getPresentation();
      presentation.setEnabled(isValidBase());
    }
  }

  public static class BaseOnThisMethodAction extends BaseOnThisElementAction {
    public BaseOnThisMethodAction() {
      super(IdeBundle.messagePointer("action.base.on.this.method"), DATA_KEY.getName(), LanguageMethodHierarchy.INSTANCE);
    }
  }

  @SuppressWarnings("UnresolvedPropertyKey")
  public static String getMethodType() {
    return IdeBundle.message("title.hierarchy.method");
  }
}
