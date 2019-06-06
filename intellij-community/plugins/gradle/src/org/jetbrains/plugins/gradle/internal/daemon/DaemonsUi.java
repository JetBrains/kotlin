// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.internal.daemon;

import com.intellij.execution.util.ListTableWithButtons;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.TableView;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.gradle.internal.impldep.org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.statistics.GradleActionsUsagesCollector;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class DaemonsUi implements Disposable {

  private final Project myProject;
  private final DaemonsTable myTable;
  private final RefreshAction myRefreshAction;
  private final StopAllAction myStopAllAction;
  private final StopSelectedAction myStopSelectedAction;
  private final JTextArea myDescriptionLabel;

  private final JPanel myContent = new JPanel();
  private MyDialogWrapper myDialog;
  private boolean myShowStopped;
  private List<? extends DaemonState> myDaemonStateList;

  public DaemonsUi(Project project) {
    myProject = project;
    myRefreshAction = new RefreshAction();
    myStopAllAction = new StopAllAction();
    myStopSelectedAction = new StopSelectedAction();
    myContent.setLayout(new BorderLayout(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP));
    myTable = new DaemonsTable();
    //TableColumn infoColumn = myTable.getTableView().getColumnModel().getColumn(2);
    //infoColumn.setCellRenderer(new LineWrapCellRenderer());
    myDescriptionLabel = new JTextArea(6, 50);
    myDescriptionLabel.setWrapStyleWord(true);
    myDescriptionLabel.setLineWrap(true);
    myDescriptionLabel.setEditable(false);

    final JScrollPane label = ScrollPaneFactory.createScrollPane(myDescriptionLabel);
    final JPanel descriptionPanel = new JPanel(new BorderLayout());

    descriptionPanel.add(label, BorderLayout.CENTER);
    JBCheckBox showStoppedCb = new JBCheckBox("show stopped");
    showStoppedCb.addActionListener(e -> {
      if (myShowStopped != showStoppedCb.isSelected()) {
        myShowStopped = showStoppedCb.isSelected();
        updateDaemonsList(myDaemonStateList);
      }
    });
    UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, showStoppedCb);
    descriptionPanel.add(showStoppedCb, BorderLayout.SOUTH);
    descriptionPanel.setBorder(IdeBorderFactory.createTitledBorder("Description", false));

    TableView<DaemonState> tableView = myTable.getTableView();
    tableView.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(@NotNull ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        DaemonState daemonState = tableView.getSelectedObject();
        if (daemonState != null) {
          String desc = daemonState.getDescription();
          myDescriptionLabel.setText(desc);
          myDescriptionLabel.setCaretPosition(0);
        }
        else {
          myDescriptionLabel.setText(null);
        }
      }
    });

    myContent.add(ScrollPaneFactory.createScrollPane(tableView), BorderLayout.CENTER);
    myContent.add(descriptionPanel, BorderLayout.SOUTH);
  }

  @Override
  public void dispose() { }

  public void show(List<? extends DaemonState> daemonStateList) {
    updateDaemonsList(daemonStateList);
    myDialog = new MyDialogWrapper();
    myDialog.show();
  }

  private void updateDaemonsList(List<? extends DaemonState> daemonStateList) {
    myDaemonStateList = daemonStateList;
    if (!myShowStopped) {
      daemonStateList = ContainerUtil.filter(daemonStateList, state -> state.getToken() != null);
    }
    else {
      daemonStateList = myDaemonStateList;
    }
    myTable.setValues(daemonStateList);
    myTable.refreshValues();
    invalidateActions();
  }

  private void invalidateActions() {
    if (myDialog != null) {
      myDialog.invalidateActions();
    }
  }

  private static class DaemonsTable extends ListTableWithButtons<DaemonState> {
    @Override
    protected ListTableModel createListModel() {
      final ColumnInfo pidColumn = new DaemonsTable.TableColumn("PID", 80) {
        @Nullable
        @Override
        public String valueOf(DaemonState daemonState) {
          return String.valueOf(daemonState.getPid());
        }

        @Nullable
        @Override
        public Comparator<DaemonState> getComparator() {
          return Comparator.comparing(DaemonState::getPid);
        }
      };
      final ColumnInfo statusColumn = new DaemonsTable.TableColumn("Status", 100) {
        @Nullable
        @Override
        public String valueOf(DaemonState daemonState) {
          return daemonState.getStatus();
        }

        @Nullable
        @Override
        public Comparator<DaemonState> getComparator() {
          return Comparator.comparing(DaemonState::getStatus);
        }
      };
      final ColumnInfo timeColumn = new DaemonsTable.TableColumn("Timestamp", 150) {
        @NotNull
        @Override
        public String valueOf(DaemonState daemonState) {
          return DateFormatUtil.formatPrettyDateTime(daemonState.getTimestamp());
        }

        @Nullable
        @Override
        public Comparator<DaemonState> getComparator() {
          return Comparator.comparing(DaemonState::getTimestamp);
        }
      };
      final ColumnInfo infoColumn = new DaemonsTable.TableColumn("Info", -1) {
        private MultiLineTableCellRenderer myRenderer;

        @NotNull
        @Override
        public String valueOf(DaemonState daemonState) {
          return daemonState.getVersion() != null ? daemonState.getVersion() : StringUtil.capitalize(daemonState.getReason());
        }

        @Override
        public TableCellRenderer getRenderer(DaemonState element) {
          String text = valueOf(element);
          if (text.length() < getTableView().getColumnModel().getColumn(2).getPreferredWidth()) {
            return super.getRenderer(element);
          }
          if (myRenderer == null) {
            myRenderer = new MultiLineTableCellRenderer();
          }
          return myRenderer;
        }
      };
      ColumnInfo[] columnInfos = new ColumnInfo[]{pidColumn, statusColumn, infoColumn, timeColumn};
      return new ListTableModel<DaemonState>(columnInfos, new ArrayList<>(), 3, SortOrder.DESCENDING);
    }

    @Override
    protected DaemonState createElement() {
      return new DaemonState(null, null, null, null, null, -1, null, null, null, null, null);
    }

    @Override
    protected boolean isEmpty(DaemonState daemonState) {
      return StringUtil.isEmpty(daemonState.getStatus()) && StringUtil.isEmpty(daemonState.getVersion());
    }

    @Override
    protected DaemonState cloneElement(DaemonState daemonState) {
      return new DaemonState(daemonState.getPid(), daemonState.getToken(),
                             daemonState.getVersion(), daemonState.getStatus(),
                             daemonState.getReason(), daemonState.getTimestamp(),
                             daemonState.getDaemonExpirationStatus(),
                             daemonState.getDaemonOpts(), daemonState.getJavaHome(),
                             daemonState.getIdleTimeout(), daemonState.getRegistryDir());
    }

    @Override
    protected boolean canDeleteElement(DaemonState selection) {
      return true;
    }

    @Override
    public List<DaemonState> getElements() {
      return super.getElements();
    }

    private abstract static class TableColumn extends ElementsColumnInfoBase<DaemonState> {

      private final int myWidth;

      TableColumn(final String name, int width) {
        super(name);
        myWidth = width;
      }

      @Override
      public boolean isCellEditable(DaemonState daemonState) {
        return false;
      }

      @Nullable
      @Override
      protected String getDescription(DaemonState daemonState) {
        return null;
      }

      @Override
      public int getWidth(JTable table) {
        return myWidth;
      }
    }
  }

  private class RefreshAction extends AbstractAction {
    RefreshAction() {
      super("Refresh", AllIcons.Actions.Refresh);
    }

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
      GradleActionsUsagesCollector.trigger(myProject, GradleActionsUsagesCollector.ActionID.refreshDaemons);
      List<DaemonState> daemonStateList = GradleDaemonServices.getDaemonsStatus();
      myTable.setValues(daemonStateList);
      updateDaemonsList(daemonStateList);
    }
  }

  private class StopAllAction extends AbstractAction {
    StopAllAction() {
      super("Stop All");
      setEnabled(false);
    }

    @Override
    public boolean isEnabled() {
      return myTable.getElements().stream().anyMatch(state -> state.getToken() != null && !"Stopped".equals(state.getStatus()));
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
      GradleActionsUsagesCollector.trigger(myProject, GradleActionsUsagesCollector.ActionID.stopAllDaemons);
      GradleDaemonServices.stopDaemons();
      List<DaemonState> daemonStateList = GradleDaemonServices.getDaemonsStatus();
      myTable.setValues(daemonStateList);
      updateDaemonsList(daemonStateList);
    }
  }

  private class StopSelectedAction extends AbstractAction {
    StopSelectedAction() {
      super("Stop Selected");
      setEnabled(false);
    }

    @Override
    public boolean isEnabled() {
      Collection<DaemonState> selection = myTable.getTableView().getSelection();
      return !selection.isEmpty() && selection.stream().anyMatch(state -> state.getToken() != null && !"Stopped".equals(state.getStatus()));
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
      GradleActionsUsagesCollector.trigger(myProject, GradleActionsUsagesCollector.ActionID.stopSelectedDaemons);
      GradleDaemonServices.stopDaemons(myTable.getTableView().getSelectedObjects());
      List<DaemonState> daemonStateList = GradleDaemonServices.getDaemonsStatus();
      myTable.setValues(daemonStateList);
      updateDaemonsList(daemonStateList);
    }
  }

  private class MyDialogWrapper extends DialogWrapper {
    {
      setTitle("Gradle Daemons");
      setModal(false);
      init();

      myTable.getTableView().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(@NotNull ListSelectionEvent e) {
          if (e.getValueIsAdjusting()) return;
          invalidateActions();
        }
      });
    }

    private AbstractAction myCloseAction;

    MyDialogWrapper() {super(true);}

    @Nullable
    @Override
    protected JComponent createNorthPanel() {
      JPanel panel = new JPanel(new BorderLayout());
      JLabel infoLabel = new JLabel(XmlStringUtil.wrapInHtml(
        "Daemons started by " +
        ApplicationNamesInfo.getInstance().getFullProductName() +
        " (or other daemons of the similar configuration) are displayed. <i>Gradle 3.0 or better supported.<i>"));
      infoLabel.setIcon(UIUtil.getInformationIcon());
      panel.add(infoLabel, BorderLayout.CENTER);
      return panel;
    }

    @Override
    protected JComponent createCenterPanel() {
      return myContent;
    }

    @Override
    protected void dispose() {
      super.dispose();
      myDialog = null;
      Disposer.dispose(DaemonsUi.this);
    }

    @Override
    protected String getDimensionServiceKey() {
      return "GradleDaemons";
    }


    @Override
    public JComponent getPreferredFocusedComponent() {
      return myTable.getTableView();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
      return new Action[]{myStopAllAction, myStopSelectedAction, myCloseAction};
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
      return new Action[]{myRefreshAction};
    }

    @Override
    protected void createDefaultActions() {
      super.createDefaultActions();
      myCloseAction = new AbstractAction("Close") {
        @Override
        public void actionPerformed(@NotNull ActionEvent e) {
          doOKAction();
        }
      };
      myCloseAction.putValue(DialogWrapper.DEFAULT_ACTION, true);
    }

    public void invalidateActions() {
      myStopSelectedAction.setEnabled(myStopSelectedAction.isEnabled());
      myStopAllAction.setEnabled(myStopAllAction.isEnabled());
    }
  }

  private static class MultiLineTableCellRenderer extends JBList<String> implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      int width = table.getColumnModel().getColumn(column).getPreferredWidth();
      String[] data = WordUtils.wrap(String.valueOf(value), width, null, false).split("\n");
      setListData(data);
      table.setRowHeight(row, table.getRowHeight() * data.length);
      setBackground(UIUtil.getTableBackground(isSelected));
      return this;
    }
  }
}
