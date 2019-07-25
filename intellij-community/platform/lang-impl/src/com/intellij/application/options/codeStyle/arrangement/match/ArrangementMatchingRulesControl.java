/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.match;

import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.ui.ArrangementEditorAware;
import com.intellij.application.options.codeStyle.arrangement.ui.ArrangementRepresentationAware;
import com.intellij.application.options.codeStyle.arrangement.util.ArrangementListRowDecorator;
import com.intellij.application.options.codeStyle.arrangement.util.IntObjectMap;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementUiComponent;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.AbstractTableCellEditor;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Denis Zhdanov
 */
public class ArrangementMatchingRulesControl extends JBTable {
  @NotNull private static final JLabel EMPTY_RENDERER = new JLabel(ApplicationBundle.message("arrangement.text.empty.rule"));

  @NotNull protected final IntObjectMap<ArrangementListRowDecorator> myComponents   = new IntObjectMap<>();
  @NotNull private final TIntArrayList mySelectedRows = new TIntArrayList();

  @NotNull private final ArrangementMatchNodeComponentFactory myFactory;
  @NotNull protected ArrangementMatchingRuleEditor        myEditor;
  @NotNull private final RepresentationCallback           myRepresentationCallback;
  @NotNull private final MatchingRulesRendererBase        myRenderer;

  private ArrangementMatchingRulesValidator myValidator;

  private final int myMinRowHeight;
  private int myRowUnderMouse = -1;
  private int myEditorRow     = -1;
  private boolean mySkipSelectionChange;

  public ArrangementMatchingRulesControl(@NotNull ArrangementStandardSettingsManager settingsManager,
                                         @NotNull ArrangementColorsProvider colorsProvider,
                                         @NotNull RepresentationCallback callback)
  {
    super(new ArrangementMatchingRulesModel());
    myRepresentationCallback = callback;
    myFactory = new ArrangementMatchNodeComponentFactory(settingsManager, colorsProvider, this);
    myRenderer = createRender();
    setDefaultRenderer(Object.class, myRenderer);
    getColumnModel().getColumn(0).setCellEditor(new MyEditor());
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    setShowColumns(false);
    setShowGrid(false);
    setSurrendersFocusOnKeystroke(true);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

    ArrangementAtomMatchCondition condition = new ArrangementAtomMatchCondition(StdArrangementTokens.EntryType.CLASS);
    StdArrangementMatchRule rule = new StdArrangementMatchRule(new StdArrangementEntryMatcher(condition));
    ArrangementUiComponent component = myFactory.getComponent(condition, rule, true);
    myMinRowHeight = new ArrangementListRowDecorator(component, this).getPreferredSize().height;

    myEditor = new ArrangementMatchingRuleEditor(settingsManager, colorsProvider, this);
    addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        onMouseMoved(e);
      }
    });
    getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        onSelectionChange(e);
      }
    });
    getModel().addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) { onTableChange(e); }
    });
  }

  protected MatchingRulesRendererBase createRender() {
    return new MatchingRulesRendererBase();
  }

  @NotNull
  protected ArrangementMatchingRulesValidator createValidator() {
    return new ArrangementMatchingRulesValidator(getModel());
  }

  private ArrangementMatchingRulesValidator getValidator() {
    if (myValidator == null) {
      myValidator = createValidator();
    }
    return myValidator;
  }

  @NotNull
  @Override
  public ArrangementMatchingRulesModel getModel() {
    return (ArrangementMatchingRulesModel)super.getModel();
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    int id = e.getID();
    switch (id) {
      case MouseEvent.MOUSE_ENTERED: onMouseEntered(e); break;
      case MouseEvent.MOUSE_EXITED: onMouseExited(); break;
      case MouseEvent.MOUSE_RELEASED: onMouseReleased(e); break;
      case MouseEvent.MOUSE_CLICKED: onMouseClicked(e); break;
    }
    if (!e.isConsumed()) {
      super.processMouseEvent(e);
    }
  }

  private void onMouseClicked(@NotNull MouseEvent e) {
    final int count = e.getClickCount();
    if (count != 2) {
      return;
    }

    final TIntArrayList rows = getSelectedModelRows();
    if (rows.size() != 1) {
      return;
    }

    final int row = rows.get(0);
    showEditor(row);
    scrollRowToVisible(row);
  }

  private void onMouseMoved(@NotNull MouseEvent e) {
    int i = rowAtPoint(e.getPoint());
    if (i != myRowUnderMouse) {
      onMouseExited();
    }

    if (i < 0) {
      return;
    }

    if (i != myRowUnderMouse) {
      onMouseEntered(e);
    }

    ArrangementListRowDecorator decorator = myComponents.get(i);
    if (decorator == null) {
      return;
    }

    Rectangle rectangle = decorator.onMouseMove(e);
    if (rectangle != null) {
      repaintScreenBounds(rectangle);
    }
  }

  private void repaintScreenBounds(@NotNull Rectangle bounds) {
    Point location = bounds.getLocation();
    SwingUtilities.convertPointFromScreen(location, this);
    int x = location.x;
    int width = bounds.width;
    repaint(x, location.y, width, bounds.height);
  }


  private void onMouseReleased(@NotNull MouseEvent e) {
    int i = rowAtPoint(e.getPoint());
    if (i < 0) {
      return;
    }

    ArrangementListRowDecorator decorator = myComponents.get(i);
    if (decorator != null) {
      decorator.onMouseRelease(e);
    }

    if (!e.isConsumed() && myEditorRow > 0 && myEditorRow == i + 1) {
      hideEditor();
    }
  }

  private void onMouseExited() {
    if (myRowUnderMouse < 0) {
      return;
    }

    ArrangementListRowDecorator decorator = myComponents.get(myRowUnderMouse);
    if (decorator != null) {
      decorator.onMouseExited();
      repaintRows(myRowUnderMouse, myRowUnderMouse, false);
    }
    myRowUnderMouse = -1;
  }

  private void onMouseEntered(@NotNull MouseEvent e) {
    myRowUnderMouse = rowAtPoint(e.getPoint());
    ArrangementListRowDecorator decorator = myComponents.get(myRowUnderMouse);
    if (decorator != null) {
      decorator.onMouseEntered(e);
      repaintRows(myRowUnderMouse, myRowUnderMouse, false);
    }
  }

  public void runOperationIgnoreSelectionChange(@NotNull Runnable task) {
    mySkipSelectionChange = true;
    try {
      if (isEditing()) {
        getCellEditor().stopCellEditing();
      }
      task.run();
    }
    finally {
      mySkipSelectionChange = false;
      refreshEditor();
    }
  }

  private void onSelectionChange(@NotNull ListSelectionEvent e) {
    if (mySkipSelectionChange || e.getValueIsAdjusting()) {
      return;
    }
    refreshEditor();
  }

  public void removeRow(int rowIndex) {
    if (rowIndex < myEditorRow) {
      hideEditor();
    }
    getModel().removeRow(rowIndex);
  }

  public void refreshEditor() {
    ArrangementMatchingRulesModel model = getModel();
    if (myEditorRow >= model.getSize()) {
      myEditorRow = -1;
      for (int i = 0, max = model.getSize(); i < max; i++) {
        if (model.getElementAt(i) instanceof ArrangementEditorComponent) {
          myEditorRow = i;
          break;
        }
      }
    }

    if (myEditorRow < 0) {
      return;
    }

    ListSelectionModel selectionModel = getSelectionModel();
    if (selectionModel.isSelectionEmpty()) {
      hideEditor();
      return;
    }

    int selectedRow = selectionModel.getMinSelectionIndex();
    if (selectedRow != selectionModel.getMaxSelectionIndex()) {
      // More than one row is selected.
      hideEditor();
      return;
    }

    if (selectedRow != myEditorRow && selectedRow != myEditorRow - 1) {
      hideEditor();
    }
  }

  public void hideEditor() {
    if (myEditorRow < 0) {
      return;
    }
    if (isEditing()) {
      TableCellEditor editor = getCellEditor();
      if (editor != null) {
        editor.stopCellEditing();
      }
    }

    mySkipSelectionChange = true;
    try {
      ArrangementMatchingRulesModel model = getModel();
      model.removeRow(myEditorRow);
      if (myEditorRow > 0 && model.getElementAt(myEditorRow - 1) instanceof EmptyArrangementRuleComponent) {
        model.removeRow(myEditorRow - 1);
      }
    }
    finally {
      mySkipSelectionChange = false;
    }
    myEditorRow = -1;
  }

  private void onTableChange(@NotNull TableModelEvent e) {
    final int signum;
    switch (e.getType()) {
      case TableModelEvent.INSERT:
        signum = 1;
        break;
      case TableModelEvent.DELETE:
        signum = -1;
        for (int i = e.getLastRow(); i >= e.getFirstRow(); i--) {
          myComponents.remove(i);
        }
        break;
      default:
        return;
    }
    int shift = Math.abs(e.getFirstRow() - e.getLastRow() + 1) * signum;
    myComponents.shiftKeys(e.getFirstRow(), shift);
    if (myRowUnderMouse >= e.getFirstRow()) {
      myRowUnderMouse = -1;
    }
    if (getModel().getSize() > 0) {
      repaintRows(0, getModel().getSize() - 1, false);
    }
  }

  public void showEditor(int rowToEdit) {
    showEditor(myEditor, rowToEdit);
  }

  public void scrollRowToVisible(int row) {
    final Rectangle rect = getCellRect(row, 0, false);
    if (row != getEditingRow() - 1) {
      scrollRectToVisible(rect);
    }
    else {
      final Rectangle editorRect = getCellRect(row + 1, 0, false);
      if(!rect.isEmpty() && !editorRect.isEmpty()) {
        final int height = (int)(rect.getHeight() + editorRect.getHeight());
        final Rectangle visibleRect = new Rectangle((int)rect.getX(), (int)rect.getY(), (int)rect.getWidth(), height);
        scrollRectToVisible(visibleRect);
      }
    }
  }

  public void showEditor(@NotNull ArrangementMatchingRuleEditor editor, int rowToEdit) {
    if (myEditorRow == rowToEdit + 1) {
      return;
    }
    if (myEditorRow >= 0) {
      if (myEditorRow < rowToEdit) {
        rowToEdit--;
      }
      hideEditor();
    }
    myEditorRow = rowToEdit + 1;
    ArrangementEditorComponent editorComponent = new ArrangementEditorComponent(this, myEditorRow, editor);
    editorComponent.applyAvailableWidth(getWidth());
    editor.reset(rowToEdit);
    mySkipSelectionChange = true;
    try {
      getModel().insertRow(myEditorRow, new Object[]{editorComponent});
    }
    finally {
      mySkipSelectionChange = false;
    }

    Rectangle bounds = getRowsBounds(rowToEdit, myEditorRow);
    if (bounds != null) {
      myRepresentationCallback.ensureVisible(bounds);
    }

    // We can't just subscribe to the model modification events and update cached renderers automatically because we need to use
    // the cached renderer on atom condition removal (via click on 'close' button). The model is modified immediately then but
    // corresponding cached renderer is used for animation.
    editorComponent.expand();
    repaintRows(rowToEdit, getModel().getRowCount() - 1, false);
    editCellAt(myEditorRow, 0);
  }

  public void repaintRows(int first, int last, boolean rowStructureChanged) {
    for (int i = first; i <= last; i++) {
      if (rowStructureChanged) {
        myComponents.remove(i);
      }
      else {
        setRowHeight(i, myRenderer.getRendererComponent(i).getPreferredSize().height);
      }
    }
    getModel().fireTableRowsUpdated(first, last);
  }

  private Rectangle getRowsBounds(int first, int last) {
    Rectangle firstRect = getCellRect(first, 0, true);
    Rectangle lastRect = getCellRect(last, 0, true);
    return new Rectangle(firstRect.x, firstRect.y, lastRect.width, lastRect.y + lastRect.height - firstRect.y);
  }

  /**
   * @return    selected model rows sorted in descending order
   */
  @NotNull
  public TIntArrayList getSelectedModelRows() {
    mySelectedRows.clear();
    int min = selectionModel.getMinSelectionIndex();
    if (min >= 0) {
      for (int i = selectionModel.getMaxSelectionIndex();  i >= min; i--) {
        if ((myEditorRow >= 0 && i == myEditorRow - 1)
            || (i != myEditorRow && selectionModel.isSelectedIndex(i)))
        {
          mySelectedRows.add(i);
        }
      }
    }
    else if (myEditorRow > 0) {
      mySelectedRows.add(myEditorRow - 1);
    }
    return mySelectedRows;
  }

  public int getRowByRenderer(@NotNull ArrangementListRowDecorator renderer) {
    for (int i = 0, max = getModel().getSize(); i < max; i++) {
      if (myComponents.get(i) == renderer) {
        return i;
      }
    }
    return -1;
  }

  public int getEmptyRowHeight() {
    return myMinRowHeight;
  }

  @NotNull
  private JComponent adjustHeight(@NotNull JComponent component, int row) {
    int height = component.getPreferredSize().height;
    if (height < myMinRowHeight) {
      height = myMinRowHeight;
    }
    setRowHeight(row, height);
    return component;
  }

  protected class MatchingRulesRendererBase implements TableCellRenderer {

    public Component getRendererComponent(int row) {
      return getTableCellRendererComponent(ArrangementMatchingRulesControl.this, getModel().getElementAt(row), false, false, row, 0);
    }

    public boolean allowModifications(StdArrangementMatchRule rule) {
      return true;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (isEditing() && getEditingRow() == row) {
        return EMPTY_RENDERER;
      }
      if (value instanceof ArrangementRepresentationAware) {
        return adjustHeight(((ArrangementRepresentationAware)value).getComponent(), row);
      }

      ArrangementListRowDecorator component = myComponents.get(row);
      if (component == null) {
        if (!(value instanceof StdArrangementMatchRule)) {
          return EMPTY_RENDERER;
        }
        StdArrangementMatchRule rule = (StdArrangementMatchRule)value;
        final boolean allowModifications = allowModifications(rule);
        ArrangementUiComponent ruleComponent = myFactory.getComponent(rule.getMatcher().getCondition(), rule, allowModifications);
        component = new ArrangementListRowDecorator(ruleComponent, ArrangementMatchingRulesControl.this);
        component.setError(getValidator().validate(row));
        myComponents.set(row, component);
      }

      component.setUnderMouse(myRowUnderMouse == row);
      component.setRowIndex((myEditorRow >= 0 && row > myEditorRow) ? row : row + 1);
      component.setSelected(getSelectionModel().isSelectedIndex(row) || (myEditorRow >= 0 && row == myEditorRow - 1));
      component.setBeingEdited(myEditorRow >= 0 && myEditorRow == row + 1);
      boolean showSortIcon = value instanceof StdArrangementMatchRule
                             && StdArrangementTokens.Order.BY_NAME.equals(((StdArrangementMatchRule)value).getOrderType());
      component.setShowSortIcon(showSortIcon);
      return component.getUiComponent();
    }
  }

  private class MyEditor extends AbstractTableCellEditor {

    private int myRow;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      if (value instanceof ArrangementEditorAware) {
        myRow = row;
        return ((ArrangementEditorAware)value).getComponent();
      }
      return null;
    }

    @Override
    public Object getCellEditorValue() {
      return myRow < getModel().getSize() ? getModel().getElementAt(myRow) : null;
    }
  }

  public interface RepresentationCallback {
    void ensureVisible(@NotNull Rectangle r);
  }
}
