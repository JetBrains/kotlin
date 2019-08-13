// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.todo.configurable;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.todo.TodoFilter;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.search.TodoPattern;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class FilterDialog extends DialogWrapper {
  private final TodoFilter myFilter;
  private final int myFilterIndex;
  private final List<? extends TodoFilter> myFilters;

  private final JTextField myNameField;
  private final JScrollPane myPatternsScrollPane;

  /**
   * @param parent      parent component.
   * @param filter      filter to be edited.
   * @param filterIndex index of {@code filter} in the {@code filters}. This parameter is
   *                    needed to not compare filter with itself when validating.
   * @param filters     all already configured filters. This parameter is used to
   * @param patterns    all patterns available in this filter.
   */
  FilterDialog(Component parent, TodoFilter filter, int filterIndex, List<? extends TodoFilter> filters, List<TodoPattern> patterns) {
    super(parent, true);
    setTitle(IdeBundle.message("title.add.todo.filter"));
    myFilter = filter;
    myFilterIndex = filterIndex;
    myFilters = filters;
    myNameField = new JBTextField(filter.getName());
    CheckBoxList<TodoPattern> patternsList = new CheckBoxList<>();
    patternsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    patternsList.setCheckBoxListListener((int index, boolean value) -> {
      if (value) {
        myFilter.addTodoPattern(patternsList.getItemAt(index));
      }
      else {
        myFilter.removeTodoPattern(patternsList.getItemAt(index));
      }
    });
    for (TodoPattern pattern : patterns) {
      patternsList.addItem(pattern, pattern.getPatternString(), myFilter.contains(pattern));
    }
    if (patternsList.getItemsCount() > 0) {
      patternsList.setSelectedIndex(0);
    }
    myPatternsScrollPane = ScrollPaneFactory.createScrollPane(patternsList);
    myPatternsScrollPane.setMinimumSize(new Dimension(300, -1));
    init();
  }

  @NotNull
  @Override
  protected List<ValidationInfo> doValidateAll() {
    List<ValidationInfo> result = new ArrayList<>();
    String filterName = getNewFilterName();
    if (filterName.isEmpty()) {
      result.add(new ValidationInfo(IdeBundle.message("error.filter.name.should.be.specified"), myNameField));
    }
    else {
      for (int i = 0; i < myFilters.size(); i++) {
        TodoFilter filter = myFilters.get(i);
        if (myFilterIndex != i && filterName.equals(filter.getName())) {
          result.add(new ValidationInfo(IdeBundle.message("error.filter.with.the.same.name.already.exists"), myNameField));
        }
      }
    }
    if (myFilter.isEmpty()) {
      result.add(new ValidationInfo(IdeBundle.message("error.filter.should.contain.at.least.one.pattern"), myPatternsScrollPane));
    }
    return result;
  }

  @NotNull
  private String getNewFilterName() {
    return myNameField.getText().trim();
  }

  @Override
  protected void doOKAction() {
    myFilter.setName(getNewFilterName());
    super.doOKAction();
  }

  @Override
  protected String getHelpId() {
    return "reference.idesettings.todo.editfilter";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel patternListPanel = new JPanel(new BorderLayout());
    patternListPanel.setBorder(IdeBorderFactory.createTitledBorder(IdeBundle.message("group.todo.filter.patterns")));
    patternListPanel.add(myPatternsScrollPane, BorderLayout.CENTER);
    return FormBuilder.createFormBuilder()
      .addLabeledComponent(IdeBundle.message("label.todo.filter.name"), myNameField)
      .addComponentFillVertically(patternListPanel, 0).getPanel();
  }
}