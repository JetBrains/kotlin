// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.scopeChooser;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.FlattenModulesToggleAction;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.DependencyUISettings;
import com.intellij.packageDependencies.ui.*;
import com.intellij.psi.search.scope.packageSet.*;
import com.intellij.ui.*;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ColorIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.ExpandVetoException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ScopeEditorPanel {

  private JPanel myButtonsPanel;
  private RawCommandLineEditor myPatternField;
  private JPanel myTreeToolbar;
  private final Tree myPackageTree;
  private JPanel myPanel;
  private JPanel myTreePanel;
  private JLabel myMatchingCountLabel;
  private JPanel myLegendPanel;

  private final Project myProject;
  private final TreeExpansionMonitor myTreeExpansionMonitor;
  private final Marker myTreeMarker;
  private PackageSet myCurrentScope = null;
  private boolean myIsInUpdate = false;
  private String myErrorMessage;
  private Future<?> myUpdateAlarm = CompletableFuture.completedFuture(null);

  private JLabel myCaretPositionLabel;
  private int myCaretPosition = 0;
  private JPanel myMatchingCountPanel;
  private JPanel myPositionPanel;
  private JLabel myRecursivelyIncluded;
  private JLabel myPartiallyIncluded;
  private PanelProgressIndicator myCurrentProgress;
  private NamedScopesHolder myHolder;

  private final MyAction myInclude = new MyAction("button.include", this::includeSelected);
  private final MyAction myIncludeRec = new MyAction("button.include.recursively", this::includeSelected);
  private final MyAction myExclude = new MyAction("button.exclude", this::excludeSelected);
  private final MyAction myExcludeRec = new MyAction("button.exclude.recursively", this::excludeSelected);

  public ScopeEditorPanel(@NotNull final Project project, @NotNull NamedScopesHolder holder) {
    myProject = project;
    myHolder = holder;

    myPackageTree = new Tree(new RootNode(project));

    myButtonsPanel.add(createActionsPanel());

    myTreePanel.setLayout(new BorderLayout());
    myTreePanel.add(ScrollPaneFactory.createScrollPane(myPackageTree), BorderLayout.CENTER);

    myTreeToolbar.setLayout(new BorderLayout());
    myTreeToolbar.add(createTreeToolbar(), BorderLayout.WEST);

    myTreeExpansionMonitor = PackageTreeExpansionMonitor.install(myPackageTree, myProject);

    myTreeMarker = new Marker() {
      @Override
      public boolean isMarked(@NotNull VirtualFile file) {
        return myCurrentScope != null && (myCurrentScope instanceof PackageSetBase ? ((PackageSetBase)myCurrentScope).contains(file, project, myHolder)
                                                                                   : myCurrentScope.contains(PackageSetBase.getPsiFile(file, myProject), myHolder));
      }
    };

    myPatternField.setDialogCaption("Pattern");
    myPatternField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void textChanged(@NotNull DocumentEvent event) {
        onTextChange();
      }
    });

    myPatternField.getTextField().addCaretListener(new CaretListener() {
      @Override
      public void caretUpdate(CaretEvent e) {
        myCaretPosition = e.getDot();
        updateCaretPositionText();
      }
    });

    myPatternField.getTextField().addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        if (myErrorMessage != null) {
          myPositionPanel.setVisible(true);
          myPanel.revalidate();
        }
      }

      @Override
      public void focusLost(FocusEvent e) {
        if (!myPatternField.getEditorField().isExpanded()) {
          myPositionPanel.setVisible(false);
          myPanel.revalidate();
        }
      }
    });

    initTree(myPackageTree);
    new UiNotifyConnector(myPanel, new Activatable() {
      @Override
      public void showNotify() {
      }

      @Override
      public void hideNotify() {
        cancelCurrentProgress();
      }
    });
    myPartiallyIncluded.setIcon(JBUI.scale(new ColorIcon(10, MyTreeCellRenderer.PARTIAL_INCLUDED)));
    myRecursivelyIncluded.setIcon(JBUI.scale(new ColorIcon(10, MyTreeCellRenderer.WHOLE_INCLUDED)));
  }

  private void updateCaretPositionText() {
    if (myErrorMessage != null) {
      myCaretPositionLabel.setText(IdeBundle.message("label.scope.editor.caret.position", myCaretPosition + 1));
    }
    else {
      myCaretPositionLabel.setText("");
    }
    myPositionPanel.setVisible(myErrorMessage != null);
    myCaretPositionLabel.setVisible(myErrorMessage != null);
    myPanel.revalidate();
  }

  public JPanel getPanel() {
    return myPanel;
  }

  public JPanel getTreePanel(){
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(myTreePanel, BorderLayout.CENTER);
    panel.add(myLegendPanel, BorderLayout.SOUTH);
    return panel;
  }

  public JPanel getTreeToolbar() {
    return myTreeToolbar;
  }

  private void onTextChange() {
    if (!myIsInUpdate) {
      myUpdateAlarm.cancel(false);
      cancelCurrentProgress();
      final String text = myPatternField.getText();
      myCurrentScope = new InvalidPackageSet(text);
      try {
        if (!StringUtil.isEmpty(text)) {
          myCurrentScope = PackageSetFactory.getInstance().compile(text);
        }
        myErrorMessage = null;
      }
      catch (Exception e) {
        myErrorMessage = e.getMessage();
        showErrorMessage();
      }
      rebuild(false);
    }
    else if (!invalidScopeInside(myCurrentScope)){
      myErrorMessage = null;
    }
  }

  private void createUIComponents() {
    myPatternField = new RawCommandLineEditor(text -> Arrays.asList(text.split("\\|\\|")),
                                              strings -> StringUtil.join(strings, "||"));
  }

  private static boolean invalidScopeInside(PackageSet currentScope) {
    if (currentScope instanceof InvalidPackageSet) return true;
    if (currentScope instanceof CompoundPackageSet) {
      return ContainerUtil.or(((CompoundPackageSet)currentScope).getSets(), s->invalidScopeInside(s));
    }
    if (currentScope instanceof ComplementPackageSet) {
      return invalidScopeInside(((ComplementPackageSet)currentScope).getComplementarySet());
    }
    return false;
  }

  private void showErrorMessage() {
    myMatchingCountLabel.setText(StringUtil.capitalize(myErrorMessage));
    myMatchingCountLabel.setForeground(JBColor.red);
    myMatchingCountLabel.setToolTipText(myErrorMessage);
  }

  private JComponent createActionsPanel() {
    myPackageTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        List<PackageSet> selection = getSelectedSets(false);
        myInclude.setSelection(selection);
        myExclude.setSelection(selection);

        List<PackageSet> recursive = getSelectedSets(true);
        myIncludeRec.setSelection(recursive);
        myExcludeRec.setSelection(recursive);
      }
    });

    JPanel buttonsPanel = new JPanel(new VerticalLayout(5));
    buttonsPanel.add(new JButton(myInclude));
    buttonsPanel.add(new JButton(myIncludeRec));
    buttonsPanel.add(new JButton(myExclude));
    buttonsPanel.add(new JButton(myExcludeRec));
    return buttonsPanel;
  }

  private void excludeSelected(@NotNull List<PackageSet> selected) {
    for (PackageSet set : selected) {
      if (myCurrentScope == null) {
        myCurrentScope = new ComplementPackageSet(set);
      }
      else if (myCurrentScope instanceof InvalidPackageSet) {
        myCurrentScope = StringUtil.isEmpty(myCurrentScope.getText()) ? new ComplementPackageSet(set) : IntersectionPackageSet.create(myCurrentScope, new ComplementPackageSet(set));
      }
      else {
        final boolean[] append = {true};
        final PackageSet simplifiedScope = processComplementaryScope(myCurrentScope, set, false, append);
        if (!append[0]) {
          myCurrentScope = simplifiedScope;
        }
        else if (simplifiedScope == null) {
          myCurrentScope = new ComplementPackageSet(set);
        }
        else {
          PackageSet[] sets = simplifiedScope instanceof IntersectionPackageSet ?
                              ((IntersectionPackageSet)simplifiedScope).getSets() :
                              new PackageSet[]{simplifiedScope};

          myCurrentScope = IntersectionPackageSet.create(ArrayUtil.append(sets, new ComplementPackageSet(set)));
        }
      }
    }
    rebuild(true);
  }

  private void includeSelected(@NotNull List<PackageSet> selected) {
    for (PackageSet set : selected) {
      if (myCurrentScope == null) {
        myCurrentScope = set;
      }
      else if (myCurrentScope instanceof InvalidPackageSet) {
        myCurrentScope = StringUtil.isEmpty(myCurrentScope.getText()) ? set : UnionPackageSet.create(myCurrentScope, set);
      }
      else {
        final boolean[] append = {true};
        final PackageSet simplifiedScope = processComplementaryScope(myCurrentScope, set, true, append);
        if (!append[0]) {
          myCurrentScope = simplifiedScope;
        }
        else if (simplifiedScope == null) {
          myCurrentScope = set;
        }
        else {
          PackageSet[] sets = simplifiedScope instanceof UnionPackageSet ?
                              ((UnionPackageSet)simplifiedScope).getSets() :
                              new PackageSet[]{simplifiedScope};
          myCurrentScope = UnionPackageSet.create(ArrayUtil.append(sets, set));
        }
      }
    }
    rebuild(true);
  }

  @Nullable
  static PackageSet processComplementaryScope(@NotNull PackageSet current, PackageSet added, boolean checkComplementSet, boolean[] append) {
    final String text = added.getText();
    if (current instanceof ComplementPackageSet &&
        Comparing.strEqual(((ComplementPackageSet)current).getComplementarySet().getText(), text)) {
      if (checkComplementSet) {
        append[0] = false;
      }
      return null;
    }
    if (Comparing.strEqual(current.getText(), text)) {
      if (!checkComplementSet) {
        append[0] = false;
      }
      return null;
    }

    if (current instanceof UnionPackageSet) {
      PackageSet[] sets = ((UnionPackageSet)current).getSets();
      PackageSet[] processed = ContainerUtil.mapNotNull(sets, s -> processComplementaryScope(s, added, checkComplementSet, append), new PackageSet[0]);
      return processed.length == 0 ? null : UnionPackageSet.create(processed);
    }

    if (current instanceof IntersectionPackageSet) {
      PackageSet[] sets = ((IntersectionPackageSet)current).getSets();
      PackageSet[] processed = ContainerUtil.mapNotNull(sets, s -> processComplementaryScope(s, added, checkComplementSet, append), new PackageSet[0]);
      return processed.length == 0 ? null : IntersectionPackageSet.create(processed);
    }

    return current;
  }

  @Nullable
  private ArrayList<PackageSet> getSelectedSets(boolean recursively) {
    int[] rows = myPackageTree.getSelectionRows();
    if (rows == null) return null;
    final ArrayList<PackageSet> result = new ArrayList<>();
    for (int row : rows) {
      final PackageDependenciesNode node = (PackageDependenciesNode)myPackageTree.getPathForRow(row).getLastPathComponent();
      final PackageSet set = PatternDialectProvider.getInstance(DependencyUISettings.getInstance().SCOPE_TYPE).createPackageSet(node, recursively);
      if (set != null) {
        result.add(set);
      }
    }
    return result;
  }


  private JComponent createTreeToolbar() {
    final DefaultActionGroup group = new DefaultActionGroup();
    final Runnable update = () -> rebuild(true);
    if (ProjectViewDirectoryHelper.getInstance(myProject).supportsFlattenPackages()) {
      group.add(new FlattenPackagesAction(update));
    }
    final List<PatternDialectProvider> dialectProviders = PatternDialectProvider.EP_NAME.getExtensionList();
    for (PatternDialectProvider provider : dialectProviders) {
      for (AnAction action : provider.createActions(myProject, update)) {
        group.add(action);
      }
    }
    group.add(new ShowFilesAction(update));
    final Module[] modules = ModuleManager.getInstance(myProject).getModules();
    if (modules.length > 1) {
      group.add(new ShowModulesAction(update));
      if (ModuleManager.getInstance(myProject).hasModuleGroups()) {
        group.add(new ShowModuleGroupsAction(update));
      }
      group.add(createFlattenModulesAction(update));
    }
    group.add(new FilterLegalsAction(update));

    if (dialectProviders.size() > 1) {
      group.add(new ChooseScopeTypeAction(update));
    }

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("ScopeEditor", group, true);
    return toolbar.getComponent();
  }

  @NotNull
  private FlattenModulesToggleAction createFlattenModulesAction(Runnable update) {
    return new FlattenModulesToggleAction(myProject, () -> DependencyUISettings.getInstance().UI_SHOW_MODULES,
                                          () -> !DependencyUISettings.getInstance().UI_SHOW_MODULE_GROUPS, value -> {
      DependencyUISettings.getInstance().UI_SHOW_MODULE_GROUPS = !value;
      update.run();
    });
  }

  private void rebuild(final boolean updateText, @Nullable final Runnable runnable, final boolean requestFocus, final int delayMillis){
    myUpdateAlarm.cancel(false);
    final Runnable request = () -> {
      if (updateText) {
        final String text = myCurrentScope != null ? myCurrentScope.getText() : null;
        SwingUtilities.invokeLater(() -> {
          try {
            myIsInUpdate = true;
            myPatternField.setText(text);
          }
          finally {
            myIsInUpdate = false;
          }
        });
      }

      try {
        if (!myProject.isDisposed()) {
          updateTreeModel(requestFocus);
        }
      }
      catch (ProcessCanceledException e) {
        return;
      }
      if (runnable != null) {
        runnable.run();
      }
    };
    myUpdateAlarm = AppExecutorUtil.getAppScheduledExecutorService().schedule(request, delayMillis, TimeUnit.MILLISECONDS);
  }

  private void rebuild(final boolean updateText) {
    rebuild(updateText, null, true, 300);
  }

  public void setHolder(NamedScopesHolder holder) {
    myHolder = holder;
  }

  private void initTree(Tree tree) {
    tree.setCellRenderer(new MyTreeCellRenderer());
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);

    TreeUtil.installActions(tree);
    SmartExpander.installOn(tree);
    new TreeSpeedSearch(tree);
    tree.addTreeWillExpandListener(new TreeWillExpandListener() {
      @Override
      public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        ((PackageDependenciesNode)event.getPath().getLastPathComponent()).sortChildren();
      }

      @Override
      public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
      }
    });

    PopupHandler.installUnknownPopupHandler(tree, createTreePopupActions(), ActionManager.getInstance());
  }

  private ActionGroup createTreePopupActions() {
    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    addAction(actionGroup, myInclude);
    addAction(actionGroup, myIncludeRec);
    addAction(actionGroup, myExclude);
    addAction(actionGroup, myExcludeRec);
    return actionGroup;
  }

  private void updateTreeModel(final boolean requestFocus) throws ProcessCanceledException {
    PanelProgressIndicator progress = createProgressIndicator(requestFocus);
    progress.setBordersVisible(false);
    myCurrentProgress = progress;
    Runnable updateModel = () -> {
      final ProcessCanceledException [] ex = new ProcessCanceledException[1];
      ApplicationManager.getApplication().runReadAction(() -> {
        if (myProject.isDisposed()) return;
        try {
          myTreeExpansionMonitor.freeze();
          final TreeModel model = PatternDialectProvider.getInstance(DependencyUISettings.getInstance().SCOPE_TYPE).createTreeModel(myProject, myTreeMarker);
          ((PackageDependenciesNode)model.getRoot()).sortChildren();
          if (myErrorMessage == null) {
            String message = IdeBundle.message("label.scope.contains.files", model.getMarkedFileCount(), model.getTotalFileCount());
            if (FilePatternPackageSet.SCOPE_FILE.equals(DependencyUISettings.getInstance().SCOPE_TYPE)) {
              message = UIUtil.toHtml(message + "<br/>(Non-project files are not shown)");
            }
            myMatchingCountLabel.setText(message);
            myMatchingCountLabel.setForeground(new JLabel().getForeground());
          }
          else {
            showErrorMessage();
          }

          SwingUtilities.invokeLater(() -> { //not under progress
            myPackageTree.setModel(model);
            myTreeExpansionMonitor.restore();
          });
        } catch (ProcessCanceledException e) {
          ex[0] = e;
        }
        finally {
          myCurrentProgress = null;
          //update label
          setToComponent(myMatchingCountLabel, requestFocus);
        }
      });
      if (ex[0] != null) {
        throw ex[0];
      }
    };
    ProgressManager.getInstance().runProcess(updateModel, progress);
  }

  private PanelProgressIndicator createProgressIndicator(final boolean requestFocus) {
    return new MyPanelProgressIndicator(requestFocus);
  }

  public void cancelCurrentProgress(){
    if (myCurrentProgress != null){
      myCurrentProgress.cancel();
    }
  }

  public void apply() throws ConfigurationException {
  }

  public PackageSet getCurrentScope() {
    return myCurrentScope;
  }

  public String getPatternText() {
     return myPatternField.getText();
   }

  public void reset(PackageSet packageSet, @Nullable Runnable runnable) {
    myCurrentScope = packageSet;
    myPatternField.setText(myCurrentScope == null ? "" : myCurrentScope.getText());
    rebuild(false, runnable, false, 0);
  }

  private void setToComponent(final JComponent cmp, final boolean requestFocus) {
    myMatchingCountPanel.removeAll();
    myMatchingCountPanel.add(cmp, BorderLayout.CENTER);
    myMatchingCountPanel.revalidate();
    myMatchingCountPanel.repaint();
    if (requestFocus) {
      SwingUtilities.invokeLater(() -> myPatternField.getTextField().requestFocusInWindow());
    }
  }

  public void restoreCanceledProgress() {
    if (myIsInUpdate) {
      rebuild(false);
    }
  }

  public void clearCaches() {
    FileTreeModelBuilder.clearCaches(myProject);
  }

  private static class MyTreeCellRenderer extends ColoredTreeCellRenderer {
    private static final Color WHOLE_INCLUDED = new JBColor(new Color(10, 119, 0), new Color(0xA5C25C));
    private static final Color PARTIAL_INCLUDED = new JBColor(new Color(0, 50, 160), DarculaColors.BLUE);

    @Override
    public void customizeCellRenderer(JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      if (value instanceof PackageDependenciesNode) {
        PackageDependenciesNode node = (PackageDependenciesNode)value;
        setIcon(node.getIcon());

        setForeground(UIUtil.getTreeForeground(selected, hasFocus));
        if (!(selected && hasFocus) && node.hasMarked() && !DependencyUISettings.getInstance().UI_FILTER_LEGALS) {
          setForeground(node.hasUnmarked() ? PARTIAL_INCLUDED : WHOLE_INCLUDED);
        }
        append(node.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        final String locationString = node.getComment();
        if (!StringUtil.isEmpty(locationString)) {
          append(" (" + locationString + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
      }
    }
  }

  private static final class ChooseScopeTypeAction extends ComboBoxAction{
    private final Runnable myUpdate;

    ChooseScopeTypeAction(final Runnable update) {
      myUpdate = update;
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(final JComponent button) {
      final DefaultActionGroup group = new DefaultActionGroup();
      for (final PatternDialectProvider provider : PatternDialectProvider.EP_NAME.getExtensionList()) {
        group.add(new AnAction(provider.getDisplayName()) {
          @Override
          public void actionPerformed(@NotNull final AnActionEvent e) {
            DependencyUISettings.getInstance().SCOPE_TYPE = provider.getShortName();
            myUpdate.run();
          }
        });
      }
      return group;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      super.update(e);
      final PatternDialectProvider provider = PatternDialectProvider.getInstance(DependencyUISettings.getInstance().SCOPE_TYPE);
      e.getPresentation().setText(provider.getDisplayName());
      e.getPresentation().setIcon(provider.getIcon());
    }
  }

  private final class FilterLegalsAction extends ToggleAction {
    private final Runnable myUpdate;

    FilterLegalsAction(final Runnable update) {
      super(IdeBundle.message("action.show.included.only"),
            IdeBundle.message("action.description.show.included.only"), AllIcons.General.Filter);
      myUpdate = update;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
      return DependencyUISettings.getInstance().UI_FILTER_LEGALS;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean flag) {
      DependencyUISettings.getInstance().UI_FILTER_LEGALS = flag;
      UIUtil.setEnabled(myLegendPanel, !flag, true);
      myUpdate.run();
    }
  }

  protected class MyPanelProgressIndicator extends PanelProgressIndicator {
    private final boolean myRequestFocus;

    public MyPanelProgressIndicator(final boolean requestFocus) {
      //noinspection Convert2Lambda
      super(new Consumer<JComponent>() {
        @Override
        public void consume(final JComponent component) {
          setToComponent(component, requestFocus);
        }
      });
      myRequestFocus = requestFocus;
    }

    @Override
    public void stop() {
      super.stop();
      setToComponent(myMatchingCountLabel, myRequestFocus);
    }

    @Override
    public String getText() { //just show non-blocking progress
      return null;
    }

    @Override
    public String getText2() {
      return null;
    }
  }

  private static void addAction(@NotNull DefaultActionGroup group, @NotNull MyAction action) {
    group.add(new DumbAwareAction(String.valueOf(action.getValue(Action.NAME))) {
      @Override
      public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(action.isEnabled());
      }

      @Override
      public void actionPerformed(@NotNull AnActionEvent event) {
        action.actionPerformed(null);
      }
    });
  }

  private static final class MyAction extends AbstractAction {
    private final Consumer<List<PackageSet>> consumer;
    private List<PackageSet> selection;

    private MyAction(@NotNull @PropertyKey(resourceBundle = IdeBundle.BUNDLE) String key, @NotNull Consumer<List<PackageSet>> consumer) {
      super(IdeBundle.message(key));
      setEnabled(false);
      this.consumer = consumer;
    }

    void setSelection(@Nullable List<PackageSet> selection) {
      this.selection = selection;
      setEnabled(selection != null && !selection.isEmpty());
    }

    @Override
    public void actionPerformed(@Nullable ActionEvent event) {
      if (selection != null && !selection.isEmpty()) consumer.consume(selection);
    }
  }
}
