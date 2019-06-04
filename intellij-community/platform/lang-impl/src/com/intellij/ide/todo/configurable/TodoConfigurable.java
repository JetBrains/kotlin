// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.todo.configurable;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.todo.TodoConfiguration;
import com.intellij.ide.todo.TodoFilter;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.search.TodoAttributesUtil;
import com.intellij.psi.search.TodoPattern;
import com.intellij.ui.*;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.table.IconTableCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladimir Kondratyev
 */
public class TodoConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private static final int HEADER_GAP = JBUIScale.scale(20);
  /*
   * UI resources
   */
  private JPanel myPanel;
  private JCheckBox myMultiLineCheckBox;
  private JBTable myPatternsTable;
  private JBTable myFiltersTable;
  protected final List<TodoPattern> myPatterns;
  private final PatternsTableModel myPatternsModel;
  protected final List<TodoFilter> myFilters;
  private final FiltersTableModel myFiltersModel;

  /**
   * Invoked by reflection
   */
  public TodoConfigurable() {
    myPatterns = new ArrayList<>();
    myFilters = new ArrayList<>();
    myFiltersModel = new FiltersTableModel(myFilters);
    myPatternsModel = new PatternsTableModel(myPatterns);
  }

  protected boolean arePatternsModified() {
    TodoConfiguration todoConfiguration = TodoConfiguration.getInstance();
    TodoPattern[] initialPatterns = getTodoPatternsToDisplay(todoConfiguration);
    if (initialPatterns.length != myPatterns.size()) {
      return true;
    }
    for (TodoPattern initialPattern : initialPatterns) {
      if (!myPatterns.contains(initialPattern)) {
        return true;
      }
    }
    return false;
  }

  protected boolean areFiltersModified() {
    TodoConfiguration todoConfiguration = TodoConfiguration.getInstance();
    TodoFilter[] initialFilters = todoConfiguration.getTodoFilters();
    if (initialFilters.length != myFilters.size()) {
      return true;
    }
    for (TodoFilter initialFilter : initialFilters) {
      if (!myFilters.contains(initialFilter)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isModified() {
    // This method is always invoked before close configuration dialog or leave "ToDo" page.
    // So it's a good place to commit all changes.
    stopEditing();
    return TodoConfiguration.getInstance().isMultiLine() != myMultiLineCheckBox.isSelected() ||
           arePatternsModified() || areFiltersModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    stopEditing();
    TodoConfiguration.getInstance().setMultiLine(myMultiLineCheckBox.isSelected());
    if (arePatternsModified()) {
      TodoPattern[] patterns = myPatterns.toArray(new TodoPattern[0]);
      TodoConfiguration.getInstance().setTodoPatterns(patterns);
    }
    if (areFiltersModified()) {
      TodoFilter[] filters = myFilters.toArray(new TodoFilter[0]);
      TodoConfiguration.getInstance().setTodoFilters(filters);
    }
  }

  @Override
  public void disposeUIResources() {
    myPanel = null;
    myPatternsModel.removeTableModelListener(myPatternsTable);
    myPatternsTable = null;

    myFiltersModel.removeTableModelListener(myFiltersTable);
    myFiltersTable = null;
  }

  @Override
  public JComponent createComponent() {
    myMultiLineCheckBox = new JCheckBox(IdeBundle.message("label.todo.multiline"));

    myPatternsTable = new JBTable(myPatternsModel);
    myPatternsTable.getEmptyText().setText(IdeBundle.message("text.todo.no.patterns"));
    TableColumn typeColumn = myPatternsTable.getColumnModel().getColumn(0);
    JTableHeader tableHeader = myPatternsTable.getTableHeader();
    FontMetrics headerFontMetrics = tableHeader.getFontMetrics(tableHeader.getFont());
    int typeColumnWidth = headerFontMetrics.stringWidth(myPatternsTable.getColumnName(0) + HEADER_GAP);
    typeColumn.setPreferredWidth(typeColumnWidth);
    typeColumn.setMinWidth(typeColumnWidth);
    typeColumn.setCellRenderer(new IconTableCellRenderer<Icon>() {
      @NotNull
      @Override
      protected Icon getIcon(@NotNull Icon value, JTable table, int row) {
        return value;
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
        super.getTableCellRendererComponent(table, value, selected, focus, row, column);
        setText("");
        return this;
      }

      @Override
      protected boolean isCenterAlignment() {
        return true;
      }
    });

    // Column "Case Sensitive"
    TableColumn todoCaseSensitiveColumn = myPatternsTable.getColumnModel().getColumn(1);
    int caseSensitiveColumnWidth = headerFontMetrics.stringWidth(myPatternsTable.getColumnName(1)) + HEADER_GAP;
    todoCaseSensitiveColumn.setPreferredWidth(caseSensitiveColumnWidth);
    todoCaseSensitiveColumn.setMinWidth(caseSensitiveColumnWidth);
    todoCaseSensitiveColumn.setCellRenderer(new BooleanTableCellRenderer());
    todoCaseSensitiveColumn.setCellEditor(new BooleanTableCellEditor());

    // Column "Pattern"
    TodoPatternTableCellRenderer todoPatternRenderer = new TodoPatternTableCellRenderer(myPatterns);
    TableColumn patternColumn = myPatternsTable.getColumnModel().getColumn(2);
    patternColumn.setCellRenderer(todoPatternRenderer);
    patternColumn.setPreferredWidth(patternColumn.getMaxWidth());

    JPanel patternsPanel = new JPanel(new BorderLayout());
    patternsPanel.setBorder(IdeBorderFactory.createTitledBorder(IdeBundle.message("label.todo.patterns"), false));
    patternsPanel.add(ToolbarDecorator.createDecorator(myPatternsTable)
                        .setAddAction(new AnActionButtonRunnable() {
                          @Override
                          public void run(AnActionButton button) {
                            stopEditing();
                            TodoPattern pattern = new TodoPattern(TodoAttributesUtil.createDefault());
                            PatternDialog dialog = new PatternDialog(myPanel, pattern, -1, myPatterns);
                            if (!dialog.showAndGet()) {
                              return;
                            }
                            myPatterns.add(pattern);
                            int index = myPatterns.size() - 1;
                            myPatternsModel.fireTableRowsInserted(index, index);
                            myPatternsTable.getSelectionModel().setSelectionInterval(index, index);
                            myPatternsTable.scrollRectToVisible(myPatternsTable.getCellRect(index, 0, true));
                          }
                        })
                        .setEditAction(new AnActionButtonRunnable() {
                          @Override
                          public void run(AnActionButton button) {
                            editSelectedPattern();
                          }
                        })
                        .setRemoveAction(new AnActionButtonRunnable() {
                          @Override
                          public void run(AnActionButton button) {
                            stopEditing();
                            int selectedIndex = myPatternsTable.getSelectedRow();
                            if (selectedIndex < 0 || selectedIndex >= myPatternsModel.getRowCount()) {
                              return;
                            }
                            TodoPattern patternToBeRemoved = myPatterns.get(selectedIndex);
                            TableUtil.removeSelectedItems(myPatternsTable);
                            for (int i = 0; i < myFilters.size(); i++) {
                              TodoFilter filter = myFilters.get(i);
                              if (filter.contains(patternToBeRemoved)) {
                                filter.removeTodoPattern(patternToBeRemoved);
                                myFiltersModel.fireTableRowsUpdated(i, i);
                              }
                            }
                          }
                        })
                        .disableUpDownActions().createPanel(), BorderLayout.CENTER);

    // double click in "Patterns" table should also start editing of selected pattern
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        editSelectedPattern();
        return true;
      }
    }.installOn(myPatternsTable);

    // Panel with filters
    myFiltersTable = new JBTable(myFiltersModel);
    myFiltersTable.getEmptyText().setText(IdeBundle.message("text.todo.no.filters"));

    // Column "Name"
    TableColumn nameColumn = myFiltersTable.getColumnModel().getColumn(0);
    int nameColumnWidth = caseSensitiveColumnWidth + typeColumnWidth;
    nameColumn.setPreferredWidth(nameColumnWidth);
    nameColumn.setMinWidth(nameColumnWidth);
    nameColumn.setCellRenderer(new MyFilterNameTableCellRenderer());

    TableColumn patternsColumn = myFiltersTable.getColumnModel().getColumn(1);
    patternsColumn.setPreferredWidth(patternsColumn.getMaxWidth());

    JPanel filtersPanel = new JPanel(new BorderLayout());
    filtersPanel.setBorder(IdeBorderFactory.createTitledBorder(IdeBundle.message("label.todo.filters"), false));
    filtersPanel.add(ToolbarDecorator.createDecorator(myFiltersTable)
                        .setAddAction(new AnActionButtonRunnable() {
                          @Override
                          public void run(AnActionButton button) {
                            stopEditing();
                            TodoFilter filter = new TodoFilter();
                            FilterDialog dialog = new FilterDialog(myPanel, filter, -1, myFilters, myPatterns);
                            if (dialog.showAndGet()) {
                              myFilters.add(filter);
                              int index = myFilters.size() - 1;
                              myFiltersModel.fireTableRowsInserted(index, index);
                              myFiltersTable.getSelectionModel().setSelectionInterval(index, index);
                              myFiltersTable.scrollRectToVisible(myFiltersTable.getCellRect(index, 0, true));
                            }
                          }
                        })
                        .setEditAction(new AnActionButtonRunnable() {
                          @Override
                          public void run(AnActionButton button) {
                            editSelectedFilter();
                          }
                        })
                        .setRemoveAction(new AnActionButtonRunnable() {
                          @Override
                          public void run(AnActionButton button) {
                            stopEditing();
                            TableUtil.removeSelectedItems(myFiltersTable);
                          }
                        }).disableUpDownActions().createPanel());

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        editSelectedFilter();
        return true;
      }
    }.installOn(myFiltersTable);

    myPanel = FormBuilder.createFormBuilder()
                         .addComponent(myMultiLineCheckBox)
                         .addComponentFillVertically(patternsPanel, 0)
                         .addComponentFillVertically(filtersPanel, 0)
                         .getPanel();
    return myPanel;
  }

  private void editSelectedPattern() {
    stopEditing();
    int selectedIndex = myPatternsTable.getSelectedRow();
    if (selectedIndex < 0 || selectedIndex >= myPatternsModel.getRowCount()) {
      return;
    }
    TodoPattern sourcePattern = myPatterns.get(selectedIndex);
    TodoPattern pattern = sourcePattern.clone();
    PatternDialog dialog = new PatternDialog(myPanel, pattern, selectedIndex, myPatterns);
    dialog.setTitle(IdeBundle.message("title.edit.todo.pattern"));
    if (!dialog.showAndGet()) {
      return;
    }
    myPatterns.set(selectedIndex, pattern);
    myPatternsModel.fireTableRowsUpdated(selectedIndex, selectedIndex);
    myPatternsTable.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
    // Update model with patterns
    for (int i = 0; i < myFilters.size(); i++) {
      TodoFilter filter = myFilters.get(i);
      if (filter.contains(sourcePattern)) {
        filter.removeTodoPattern(sourcePattern);
        filter.addTodoPattern(pattern);
        myFiltersModel.fireTableRowsUpdated(i, i);
      }
    }
  }

  private void editSelectedFilter() {
    stopEditing();
    int selectedIndex = myFiltersTable.getSelectedRow();
    if (selectedIndex < 0 || selectedIndex >= myFiltersModel.getRowCount()) {
      return;
    }
    TodoFilter sourceFilter = myFilters.get(selectedIndex);
    TodoFilter filter = sourceFilter.clone();
    FilterDialog dialog = new FilterDialog(myPanel, filter, selectedIndex, myFilters, myPatterns);
    dialog.setTitle(IdeBundle.message("title.edit.todo.filter"));
    dialog.show();
    int exitCode = dialog.getExitCode();
    if (DialogWrapper.OK_EXIT_CODE == exitCode) {
      myFilters.set(selectedIndex, filter);
      myFiltersModel.fireTableRowsUpdated(selectedIndex, selectedIndex);
      myFiltersTable.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
    }
  }

  protected void stopEditing() {
    if (myPatternsTable.isEditing()) {
      TableCellEditor editor = myPatternsTable.getCellEditor();
      if (editor != null) {
        editor.stopCellEditing();
      }
    }
    if (myFiltersTable.isEditing()) {
      TableCellEditor editor = myFiltersTable.getCellEditor();
      if (editor != null) {
        editor.stopCellEditing();
      }
    }
  }

  @Override
  public String getDisplayName() {
    return IdeBundle.message("title.todo");
  }

  @Override
  @NotNull
  public String getHelpTopic() {
    return "preferences.toDoOptions";
  }

  @Override
  public void reset() {
    myMultiLineCheckBox.setSelected(TodoConfiguration.getInstance().isMultiLine());
    // Patterns
    myPatterns.clear();
    TodoConfiguration todoConfiguration = TodoConfiguration.getInstance();
    TodoPattern[] patterns = getTodoPatternsToDisplay(todoConfiguration);
    for (TodoPattern pattern : patterns) {
      myPatterns.add(pattern.clone());
    }
    myPatternsModel.fireTableDataChanged();
    // Filters
    myFilters.clear();
    TodoFilter[] filters = todoConfiguration.getTodoFilters();
    for (TodoFilter filter : filters) {
      myFilters.add(filter.clone());
    }
    myFiltersModel.fireTableDataChanged();
  }

  @NotNull
  protected TodoPattern[] getTodoPatternsToDisplay(TodoConfiguration todoConfiguration) {
    return todoConfiguration.getTodoPatterns();
  }

  private final class MyFilterNameTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      TodoFilter filter = myFilters.get(row);
      if (isSelected) {
        setForeground(UIUtil.getTableSelectionForeground(true));
      }
      else {
        if (filter.isEmpty()) {
          setForeground(JBColor.RED);
        }
        else {
          setForeground(UIUtil.getTableForeground());
        }
      }
      return this;
    }
  }

  @Override
  @NotNull
  public String getId() {
    return getHelpTopic();
  }

}
