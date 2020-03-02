// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.template.impl.TemplateEditorUtil;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.editable.EditablePostfixTemplateWithMultipleExpressions;
import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateEditor;
import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateExpressionCondition;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.panel.PanelGridBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ListUtil;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.DialogUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public abstract class PostfixTemplateEditorBase<Condition extends PostfixTemplateExpressionCondition> implements PostfixTemplateEditor {

  @NotNull protected final PostfixTemplateProvider myProvider;
  @NotNull protected final Editor myTemplateEditor;
  @NotNull protected final JBList<Condition> myExpressionTypesList;
  @NotNull protected final DefaultListModel<Condition> myExpressionTypesListModel;

  @NotNull protected final JBCheckBox myApplyToTheTopmostJBCheckBox;
  @NotNull protected final JPanel myEditTemplateAndConditionsPanel;

  protected class AddConditionAction extends DumbAwareAction {
    @NotNull
    private final Condition myCondition;

    public AddConditionAction(Condition condition) {
      super(condition.getPresentableName());
      myCondition = condition;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myExpressionTypesListModel.addElement(myCondition);
    }
  }

  public PostfixTemplateEditorBase(@NotNull PostfixTemplateProvider provider, boolean showExpressionTypes) {
    this(provider, createSimpleEditor(), showExpressionTypes);
  }


  public PostfixTemplateEditorBase(@NotNull PostfixTemplateProvider provider,
                                   @NotNull Editor templateEditor,
                                   boolean showExpressionTypes) {
    myProvider = provider;
    myTemplateEditor = templateEditor;

    myApplyToTheTopmostJBCheckBox = new JBCheckBox(CodeInsightBundle.message("checkbox.apply.to.the.&topmost.expression"));
    DialogUtil.registerMnemonic(myApplyToTheTopmostJBCheckBox, '&');
    myApplyToTheTopmostJBCheckBox.setBorder(JBUI.Borders.emptyBottom(UIUtil.DEFAULT_VGAP));

    myExpressionTypesListModel = JBList.createDefaultListModel();
    myExpressionTypesList = new JBList<>(myExpressionTypesListModel);
    myExpressionTypesList.setCellRenderer(SimpleListCellRenderer.create("", PostfixTemplateExpressionCondition::getPresentableName));

    PanelGridBuilder grid = UI.PanelFactory.grid().resize();
    if (showExpressionTypes) {
      JPanel expressionTypesPanel = ToolbarDecorator.createDecorator(myExpressionTypesList)
        .setAddAction(button -> showAddExpressionTypePopup(button))
        .setRemoveAction(button -> ListUtil.removeSelectedItems(myExpressionTypesList))
        .disableUpDownActions()
        .setMinimumSize(JBUI.size(-1, 300))
        .createPanel();
      grid.add(UI.PanelFactory.panel(expressionTypesPanel)
                 .withLabel(CodeInsightBundle.message("label.applicable.expression.types"))
                 .resizeY(true)
                 .moveLabelOnTop());
    }
    grid.add(UI.PanelFactory.panel(myApplyToTheTopmostJBCheckBox));
    grid.add(UI.PanelFactory.panel(myTemplateEditor.getComponent())
               .anchorLabelOn(UI.Anchor.Top)
               .withComment(CodeInsightBundle.message("comment.use.expr.variable.to.refer.target.expression"))
               .resizeY(true));
    myEditTemplateAndConditionsPanel = grid.createPanel();
  }

  @NotNull
  protected static Editor createEditor(@Nullable Project project, @NotNull Document document) {
    return TemplateEditorUtil.createEditor(false, document, project);
  }

  @NotNull
  private static Editor createSimpleEditor() {
    return createEditor(null, EditorFactory.getInstance().createDocument(""));
  }

  protected final void showAddExpressionTypePopup(@NotNull AnActionButton button) {
    DefaultActionGroup group = new DefaultActionGroup();
    fillConditions(group);
    DataContext context = DataManager.getInstance().getDataContext(button.getContextComponent());
    ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, group, context,
                                                                          JBPopupFactory.ActionSelectionAid.ALPHA_NUMBERING, true, null);
    popup.show(Objects.requireNonNull(button.getPreferredPopupPoint()));
  }

  protected abstract void fillConditions(@NotNull DefaultActionGroup group);

  public void setTemplate(@Nullable PostfixTemplate rawTemplate) {
    if (!(rawTemplate instanceof EditablePostfixTemplateWithMultipleExpressions)) return;

    //noinspection unchecked
    EditablePostfixTemplateWithMultipleExpressions<Condition> template = (EditablePostfixTemplateWithMultipleExpressions)rawTemplate;

    myExpressionTypesListModel.clear();
    for (Condition condition : template.getExpressionConditions()) {
      myExpressionTypesListModel.addElement(condition);
    }
    myApplyToTheTopmostJBCheckBox.setSelected(template.isUseTopmostExpression());
    ApplicationManager.getApplication()
                      .runWriteAction(() -> myTemplateEditor.getDocument().setText(template.getLiveTemplate().getString()));
  }

  @Override
  public String getHelpId() {
    return "reference.custom.postfix.templates";
  }

  @Override
  public void dispose() {
    TemplateEditorUtil.disposeTemplateEditor(myTemplateEditor);
  }
}
