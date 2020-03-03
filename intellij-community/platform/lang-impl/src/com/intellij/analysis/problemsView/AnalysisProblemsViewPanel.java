// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.CopyProvider;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.table.TableView;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.OpenSourceUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public abstract class AnalysisProblemsViewPanel extends SimpleToolWindowPanel implements DataProvider, CopyProvider {

  @NotNull protected final Project myProject;
  @NotNull protected final TableView<AnalysisProblem> myTable;

  @NotNull protected final AnalysisProblemsPresentationHelper myPresentationHelper;
  public AnalysisProblemsViewPanel(@NotNull Project project,
                                   @NotNull AnalysisProblemsPresentationHelper presentationHelper) {
    super(false, true);
    myProject = project;
    myPresentationHelper = presentationHelper;

    myTable = createTable();
    setToolbar(createToolbar());
    setContent(createCenterPanel());
  }

  private void popupInvoked(final Component component, final int x, final int y) {
    final DefaultActionGroup group = new DefaultActionGroup();
    if (getData(CommonDataKeys.NAVIGATABLE.getName()) != null) {
      group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE));
    }

    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY));

    final List<AnalysisProblem> selectedProblems = myTable.getSelectedObjects();
    final AnalysisProblem selectedProblem = selectedProblems.size() == 1 ? selectedProblems.get(0) : null;

    addQuickFixActions(group, selectedProblem);
    addDiagnosticMessageActions(group, selectedProblem);
    addDocumentationAction(group, selectedProblem);

    final ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLBAR, group);
    menu.getComponent().show(component, x, y);
  }

  @NotNull
  protected TableView<AnalysisProblem> createTable() {
    final TableView<AnalysisProblem> table = new TableView<>(createTableModel());

    table.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          navigate(false); // as in NewErrorTreeViewPanel
        }
      }
    });

    EditSourceOnDoubleClickHandler.install(table);

    table.addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        popupInvoked(comp, x, y);
      }
    });

    //noinspection unchecked
    ((DefaultRowSorter<AnalysisProblemsTableModel, Integer>)table.getRowSorter()).setRowFilter(myPresentationHelper.getRowFilter());

    table.getRowSorter().addRowSorterListener(e -> {
      final List<? extends RowSorter.SortKey> sortKeys = myTable.getRowSorter().getSortKeys();
      assert sortKeys.size() == 1 : sortKeys;
      ((AnalysisProblemsTableModel)myTable.getModel()).setSortKey(sortKeys.get(0));
    });

    new TableSpeedSearch(table, object -> object instanceof AnalysisProblem
                                          ? ((AnalysisProblem)object).getErrorMessage() +
                                            " " +
                                            ((AnalysisProblem)object).getPresentableLocation()
                                          : "");

    table.setShowVerticalLines(false);
    table.setShowHorizontalLines(false);
    table.setStriped(true);
    table.setRowHeight(table.getRowHeight() + JBUIScale.scale(4));

    JTableHeader tableHeader = table.getTableHeader();
    tableHeader.setPreferredSize(new Dimension(0, table.getRowHeight()));

    return table;
  }

  @NotNull
  protected AnalysisProblemsTableModel createTableModel() {
    return new AnalysisProblemsTableModel(myPresentationHelper);
  }

  protected abstract void addQuickFixActions(@NotNull DefaultActionGroup group, @Nullable AnalysisProblem problem);

  private void addDiagnosticMessageActions(@NotNull DefaultActionGroup group, @Nullable AnalysisProblem problem){
    final List<AnalysisProblem> diagnosticMessages = problem != null ? problem.getSecondaryMessages() : null;
    if (diagnosticMessages == null || diagnosticMessages.isEmpty()) return;

    group.addSeparator();
    // Reference the icon for "Jump to Source", higher in this menu group, to indicate that the action will have the same behavior
    final Icon jumpToSourceIcon = ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE).getTemplatePresentation().getIcon();
    for (AnalysisProblem diagnosticMessage : diagnosticMessages) {
      // Reference the message, trim, non-nullize, and remove a trailing period, if one exists
      String message = StringUtil.notNullize(diagnosticMessage.getErrorMessage());
      message = StringUtil.trimEnd(StringUtil.trim(message), ".");

      // Reference the Location, compute the VirtualFile
      VirtualFile vFile = diagnosticMessage.getFile();

      // Create the action for this DiagnosticMessage
      if (StringUtil.isNotEmpty(message) && vFile != null) {
        group.add(new DumbAwareAction(message, null, jumpToSourceIcon) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            final int offset = getConvertedOffset(vFile, diagnosticMessage);
            OpenSourceUtil.navigate(PsiNavigationSupport.getInstance().createNavigatable(myProject, vFile, offset));
          }
        });
      }
    }
  }

  protected int getConvertedOffset(@NotNull VirtualFile vFile, @NotNull AnalysisProblem diagnosticMessage) {
    return diagnosticMessage.getOffset();
  }

  @NotNull
  protected JComponent createToolbar() {
    final DefaultActionGroup group = new DefaultActionGroup();
    addActionsTo(group);

    return ActionManager.getInstance().createActionToolbar("InspectionProblemsView", group, false).getComponent();
  }

  @NotNull
  public AnalysisProblemsTableModel getModel() {
    return (AnalysisProblemsTableModel)myTable.getModel();
  }

  protected abstract void addActionsTo(@NotNull DefaultActionGroup group);

  @NotNull
  protected JPanel createCenterPanel() {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(ScrollPaneFactory.createScrollPane(myTable), BorderLayout.CENTER);
    return panel;
  }

  public void fireGroupingOrFilterChanged() {
    myTable.getRowSorter().allRowsChanged();
    ((AnalysisProblemsTableModel)myTable.getModel()).onFilterChanged();
    updateStatusDescription();
  }

  protected abstract void updateStatusDescription();

  @NotNull
  protected Icon getStatusIcon() {
    return AllIcons.Toolwindows.Problems;
  }

  protected void addAutoScrollToSourceAction(@NotNull final DefaultActionGroup group) {
    final AutoScrollToSourceHandler autoScrollToSourceHandler = new AutoScrollToSourceHandler() {
      @Override
      protected boolean isAutoScrollMode() {
        return myPresentationHelper.isAutoScrollToSource();
      }

      @Override
      protected void setAutoScrollMode(boolean autoScrollToSource) {
        myPresentationHelper.setAutoScrollToSource(autoScrollToSource);
      }
    };

    autoScrollToSourceHandler.install(myTable);
    group.addAction(autoScrollToSourceHandler.createToggleAction());
  }

  protected void addGroupBySeverityAction(@NotNull final DefaultActionGroup group) {
    final AnAction action = new DumbAwareToggleAction(AnalysisProblemBundle.message("group.by.severity"),
                                                      AnalysisProblemBundle.message("group.by.severity.description"),
                                                      AllIcons.Nodes.SortBySeverity) {
      @Override
      public boolean isSelected(@NotNull AnActionEvent e) {
        return myPresentationHelper.isGroupBySeverity();
      }

      @Override
      public void setSelected(@NotNull AnActionEvent e, boolean groupBySeverity) {
        myPresentationHelper.setGroupBySeverity(groupBySeverity);
        fireGroupingOrFilterChanged();
      }
    };

    group.addAction(action);
  }

  protected void createAndShowPopup(@NotNull final String title, @NotNull final JPanel jPanel) {
    final Rectangle visibleRect = myTable.getVisibleRect();
    final Point tableTopLeft = new Point(myTable.getLocationOnScreen().x + visibleRect.x, myTable.getLocationOnScreen().y + visibleRect.y);

    JBPopupFactory.getInstance()
      .createComponentPopupBuilder(jPanel, null)
      .setProject(myProject)
      .setTitle(title)
      .setMovable(true)
      .setRequestFocus(true)
      .createPopup().show(RelativePoint.fromScreen(tableTopLeft));
  }

  @Override
  public boolean isCopyVisible(@NotNull DataContext dataContext) {
    return true;
  }

  @Override
  public boolean isCopyEnabled(@NotNull DataContext dataContext) {
    return myTable.getSelectedObject() != null;
  }

  @Override
  public void performCopy(@NotNull DataContext dataContext) {
    final List<AnalysisProblem> selectedObjects = myTable.getSelectedObjects();
    final String s = StringUtil.join(selectedObjects, problem -> StringUtil.toLowerCase(problem.getSeverity()) +
                                                                 ": " +
                                                                 problem.getErrorMessage() +
                                                                 " (" +
                                                                 problem.getCode() + " at " + problem.getPresentableLocation() +
                                                                 ")", "\n");

    if (!s.isEmpty()) {
      CopyPasteManager.getInstance().setContents(new StringSelection(s));
    }
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (PlatformDataKeys.COPY_PROVIDER.is(dataId)) {
      return this;
    }
    if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
      return createNavigatable();
    }

    return null;
  }

  @Nullable
  private Navigatable createNavigatable() {
    final AnalysisProblem problem = myTable.getSelectedObject();
    if (problem != null) {
      final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(problem.getSystemIndependentPath());
      if (file != null) {
        final OpenFileDescriptor navigatable = new OpenFileDescriptor(myProject, file, problem.getOffset());
        navigatable.setScrollType(ScrollType.MAKE_VISIBLE);
        return navigatable;
      }
    }

    return null;
  }

  private void navigate(@SuppressWarnings("SameParameterValue") final boolean requestFocus) {
    final Navigatable navigatable = createNavigatable();
    if (navigatable != null && navigatable.canNavigateToSource()) {
      navigatable.navigate(requestFocus);
    }
  }

  public void clearAll() {
    ((AnalysisProblemsTableModel)myTable.getModel()).removeAll();
    updateStatusDescription();
  }

  private static void addDocumentationAction(@NotNull final DefaultActionGroup group, @Nullable AnalysisProblem problem) {
    final String url = problem != null ? problem.getUrl() : null;
    if (url == null) return;

    group.addSeparator();
    group.add(new DumbAwareAction(IdeBundle.messagePointer("action.DumbAware.DartProblemsViewPanel.text.open.documentation"),
                                  IdeBundle.messagePointer(
                                    "action.DumbAware.DartProblemsViewPanel.description.open.detailed.problem.description.in.browser"),
                                  AllIcons.Ide.External_link_arrow) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        BrowserUtil.browse(url);
      }
    });
  }
}
