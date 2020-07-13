// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions;

import com.intellij.ide.util.gotoByName.ModelDiff;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.SpeedSearchBase;
import com.intellij.ui.SpeedSearchComparator;
import com.intellij.ui.TableUtil;
import com.intellij.ui.popup.HintUpdateSupply;
import com.intellij.ui.table.JBTable;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageToPsiElementProvider;
import com.intellij.usages.UsageView;
import com.intellij.usages.impl.GroupNode;
import com.intellij.usages.impl.UsageAdapter;
import com.intellij.usages.impl.UsageNode;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ShowUsagesTable extends JBTable implements DataProvider {
  final Usage MORE_USAGES_SEPARATOR = new UsageAdapter();
  final Usage USAGES_OUTSIDE_SCOPE_SEPARATOR = new UsageAdapter();
  final Usage USAGES_FILTERED_OUT_SEPARATOR = new UsageAdapter();
  private static final int MARGIN = 2;

  private final ShowUsagesTableCellRenderer myRenderer;
  private final UsageView myUsageView;

  ShowUsagesTable(@NotNull ShowUsagesTableCellRenderer renderer, @NotNull UsageView usageView) {
    myRenderer = renderer;
    myUsageView = usageView;
    ScrollingUtil.installActions(this);
    HintUpdateSupply.installDataContextHintUpdateSupply(this);
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
      final int[] selected = getSelectedRows();
      if (selected.length == 1) {
        return getPsiElementForHint(getValueAt(selected[0], 0));
      }
    }
    else if (LangDataKeys.POSITION_ADJUSTER_POPUP.is(dataId)) {
      return PopupUtil.getPopupContainerFor(this);
    }
    else if (UsageView.USAGE_VIEW_KEY.is(dataId)) {
      return myUsageView;
    }
    return null;
  }

  @Override
  public int getRowHeight() {
    return super.getRowHeight() + 2 * MARGIN;
  }

  @NotNull
  @Override
  public Component prepareRenderer(@NotNull TableCellRenderer renderer, int row, int column) {
    Component component = super.prepareRenderer(renderer, row, column);
    if (component instanceof JComponent) {
      ((JComponent)component).setBorder(JBUI.Borders.empty(MARGIN, MARGIN, MARGIN, 0));
    }
    return component;
  }

  @NotNull
  Runnable prepareTable(@NotNull Runnable appendMoreUsageRunnable, @NotNull Runnable showInMaximalScopeRunnable) {
    SpeedSearchBase<JTable> speedSearch = new MySpeedSearch(this);
    speedSearch.setComparator(new SpeedSearchComparator(false));

    setRowHeight(PlatformIcons.CLASS_ICON.getIconHeight() + 2);
    setShowGrid(false);
    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setTableHeader(null);
    setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);
    setIntercellSpacing(new Dimension(0, 0));

    final AtomicReference<java.util.List<Object>> selectedUsages = new AtomicReference<>();
    final AtomicBoolean moreUsagesSelected = new AtomicBoolean();
    final AtomicBoolean outsideScopeUsagesSelected = new AtomicBoolean();
    final AtomicReference<ShowUsagesAction.FilteredOutUsagesNode> filteredOutUsagesSelected = new AtomicReference<>();
    getSelectionModel().addListSelectionListener(e -> {
      selectedUsages.set(null);
      outsideScopeUsagesSelected.set(false);
      moreUsagesSelected.set(false);
      filteredOutUsagesSelected.set(null);
      java.util.List<Object> usages = null;

      for (int i : getSelectedRows()) {
        Object value = getValueAt(i, 0);
        if (value instanceof UsageNode) {
          Usage usage = ((UsageNode)value).getUsage();
          if (usage == USAGES_OUTSIDE_SCOPE_SEPARATOR) {
            outsideScopeUsagesSelected.set(true);
            usages = null;
            break;
          }
          if (usage == MORE_USAGES_SEPARATOR) {
            moreUsagesSelected.set(true);
            usages = null;
            break;
          }
          if (usage == USAGES_FILTERED_OUT_SEPARATOR) {
            filteredOutUsagesSelected.set((ShowUsagesAction.FilteredOutUsagesNode)value);
            usages = null;
            break;
          }
          if (usages == null) usages = new ArrayList<>();
          usages.add(usage instanceof UsageInfo2UsageAdapter ? ((UsageInfo2UsageAdapter)usage).getUsageInfo().copy() : usage);
        }
      }

      selectedUsages.set(usages);
    });

    return () -> {
      if (moreUsagesSelected.get()) {
        appendMoreUsageRunnable.run();
        return;
      }
      if (outsideScopeUsagesSelected.get()) {
        showInMaximalScopeRunnable.run();
        return;
      }
      if (filteredOutUsagesSelected.get() != null) {
        filteredOutUsagesSelected.get().onSelected();
        return;
      }

      List<Object> usages = selectedUsages.get();
      if (usages != null) {
        for (Object usage : usages) {
          if (usage instanceof UsageInfo) {
            UsageViewUtil.navigateTo((UsageInfo)usage, true);
          }
          else if (usage instanceof Navigatable) {
            ((Navigatable)usage).navigate(true);
          }
        }
      }
    };
  }

  public boolean isSeparatorNode(@Nullable Usage node) {
    return node == USAGES_OUTSIDE_SCOPE_SEPARATOR
           ||node == MORE_USAGES_SEPARATOR
           ||node == USAGES_FILTERED_OUT_SEPARATOR;
  }

  @Nullable
  private static PsiElement getPsiElementForHint(Object selectedValue) {
    if (selectedValue instanceof UsageNode) {
      final Usage usage = ((UsageNode)selectedValue).getUsage();
      if (usage instanceof UsageInfo2UsageAdapter) {
        final PsiElement element = ((UsageInfo2UsageAdapter)usage).getElement();
        if (element != null) {
          final PsiElement view = UsageToPsiElementProvider.findAppropriateParentFrom(element);
          return view == null ? element : view;
        }
      }
    }
    return null;
  }

  private static int calcColumnCount(@NotNull List<UsageNode> data) {
    return data.isEmpty() || data.get(0) instanceof ShowUsagesAction.StringNode ? 1 : 4;
  }

  @NotNull
  MyModel setTableModel(@NotNull final List<UsageNode> data) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final int columnCount = calcColumnCount(data);
    MyModel model = getModel() instanceof MyModel ? (MyModel)getModel() : null;
    if (model == null || model.getColumnCount() != columnCount) {
      model = new MyModel(data, columnCount);
      setModel(model);

      for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
        TableColumn column = getColumnModel().getColumn(i);
        column.setPreferredWidth(0);
        column.setCellRenderer(myRenderer);
      }
    }
    return model;
  }

  private static class MySpeedSearch extends SpeedSearchBase<JTable> {
    MySpeedSearch(@NotNull ShowUsagesTable table) {
      super(table);
    }

    @Override
    protected int getSelectedIndex() {
      return getTable().getSelectedRow();
    }

    @Override
    protected int convertIndexToModel(int viewIndex) {
      return getTable().convertRowIndexToModel(viewIndex);
    }

    @Override
    protected Object @NotNull [] getAllElements() {
      return ((MyModel)getTable().getModel()).getItems().toArray();
    }

    @Override
    protected String getElementText(@NotNull Object element) {
      if (!(element instanceof UsageNode)) return element.toString();
      UsageNode node = (UsageNode)element;
      if (node instanceof ShowUsagesAction.StringNode) return "";
      Usage usage = node.getUsage();
      if (usage == getTable().MORE_USAGES_SEPARATOR || usage == getTable().USAGES_OUTSIDE_SCOPE_SEPARATOR || usage == getTable().USAGES_FILTERED_OUT_SEPARATOR) return "";
      GroupNode group = (GroupNode)node.getParent();
      String groupText = group == null ? "" : group.getGroup().getText(null);
      return groupText + usage.getPresentation().getPlainText();
    }

    @Override
    protected void selectElement(Object element, String selectedText) {
      List<UsageNode> data = ((MyModel)getTable().getModel()).getItems();
      int i = data.indexOf(element);
      if (i == -1) return;
      final int viewRow = getTable().convertRowIndexToView(i);
      getTable().getSelectionModel().setSelectionInterval(viewRow, viewRow);
      TableUtil.scrollSelectionToVisible(getTable());
    }

    private ShowUsagesTable getTable() {
      return (ShowUsagesTable)myComponent;
    }
  }

  static class MyModel extends ListTableModel<UsageNode> implements ModelDiff.Model<Object> {
    private MyModel(@NotNull List<UsageNode> data, int cols) {
      super(cols(cols), data, 0);
    }

    @NotNull
    private static ColumnInfo<UsageNode, UsageNode>[] cols(int cols) {
      ColumnInfo<UsageNode, UsageNode> o = new ColumnInfo<UsageNode, UsageNode>("") {
        @Nullable
        @Override
        public UsageNode valueOf(UsageNode node) {
          return node;
        }
      };
      List<ColumnInfo<UsageNode, UsageNode>> list = Collections.nCopies(cols, o);
      return list.toArray(ColumnInfo.emptyArray());
    }

    @Override
    public void addToModel(int idx, Object element) {
      UsageNode node = element instanceof UsageNode ? (UsageNode)element : ShowUsagesAction.createStringNode(element);

      if (idx < getRowCount()) {
        insertRow(idx, node);
      }
      else {
        addRow(node);
      }
    }

    @Override
    public void removeRangeFromModel(int start, int end) {
      for (int i=end; i>=start; i--) {
        removeRow(i);
      }
    }
  }
}
