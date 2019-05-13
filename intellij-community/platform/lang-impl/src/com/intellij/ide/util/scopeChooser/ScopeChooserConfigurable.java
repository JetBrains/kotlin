// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.util.scopeChooser;

import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.execution.ExecutionBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.search.scope.impl.CustomScopesAggregator;
import com.intellij.psi.search.scope.packageSet.*;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

public class ScopeChooserConfigurable extends MasterDetailsComponent implements SearchableConfigurable {
  @NonNls public static final String SCOPE_CHOOSER_CONFIGURABLE_UI_KEY = "ScopeChooserConfigurable.UI";
  public static final String PROJECT_SCOPES = "project.scopes";
  private final NamedScopesHolder myLocalScopesManager;
  private final NamedScopesHolder mySharedScopesManager;

  private final Project myProject;

  public ScopeChooserConfigurable(final Project project) {
    super(new ScopeChooserConfigurableState());
    myLocalScopesManager = NamedScopeManager.getInstance(project);
    mySharedScopesManager = DependencyValidationManager.getInstance(project);
    myProject = project;

    initTree();
  }

  @Override
  protected String getComponentStateKey() {
    return SCOPE_CHOOSER_CONFIGURABLE_UI_KEY;
  }

  @Override
  protected Dimension getPanelPreferredSize() {
    return JBUI.size(400, 200);
  }

  @Override
  protected MasterDetailsStateService getStateService() {
    return MasterDetailsStateService.getInstance(myProject);
  }

  @Override
  protected ArrayList<AnAction> createActions(final boolean fromPopup) {
    final ArrayList<AnAction> result = new ArrayList<>();
    result.add(new MyAddAction(fromPopup));
    result.add(new MyDeleteAction(forAll(o -> {
      if (o instanceof MyNode) {
        final NamedConfigurable namedConfigurable = ((MyNode)o).getConfigurable();
        final Object editableObject = namedConfigurable != null ? namedConfigurable.getEditableObject() : null;
        return editableObject instanceof NamedScope;
      }
      return false;
    })));
    result.add(new MyCopyAction());
    result.add(new MySaveAsAction());
    result.add(new MyMoveAction(ExecutionBundle.message("move.up.action.name"), IconUtil.getMoveUpIcon(), -1));
    result.add(new MyMoveAction(ExecutionBundle.message("move.down.action.name"), IconUtil.getMoveDownIcon(), 1));
    return result;
  }

  @Override
  public void reset() {
    myRoot.removeAllChildren();
    loadScopes(mySharedScopesManager);
    loadScopes(myLocalScopesManager);

    loadComponentState();

    final List<String> order = getScopesState().myOrder;
    TreeUtil.sortRecursively(myRoot, (o1, o2) -> {
      final int idx1 = order.indexOf(o1.getDisplayName());
      final int idx2 = order.indexOf(o2.getDisplayName());
      return idx1 - idx2;
    });

    if (getScopesState().myOrder.size() != myRoot.getChildCount()) {
      loadStateOrder();
    }

    super.reset();
  }


  @Override
  public void apply() throws ConfigurationException {
    checkForEmptyAndDuplicatedNames(ProjectBundle.message("rename.message.prefix.scope"),
                                    ProjectBundle.message("rename.scope.title"), ScopeConfigurable.class);
    checkForPredefinedNames();
    super.apply();
    processScopes();

    loadStateOrder();
  }

  private void checkForPredefinedNames() throws ConfigurationException {
    final Set<String> predefinedScopes = new HashSet<>();
    for (NamedScope scope : CustomScopesAggregator.getAllCustomScopes(myProject)) {
      predefinedScopes.add(scope.getName());
    }
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      final MyNode node = (MyNode)myRoot.getChildAt(i);
      final NamedConfigurable scopeConfigurable = node.getConfigurable();
      final String name = scopeConfigurable.getDisplayName();
      if (predefinedScopes.contains(name)) {
        selectNodeInTree(node);
        throw new ConfigurationException("Scope name equals to predefined one", ProjectBundle.message("rename.scope.title"));
      }
    }
  }

  public ScopeChooserConfigurableState getScopesState() {
    return (ScopeChooserConfigurableState)myState;
  }

  @Override
  public boolean isModified() {
    final List<String> order = getScopesState().myOrder;
    if (myRoot.getChildCount() != order.size()) return true;
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      final MyNode node = (MyNode)myRoot.getChildAt(i);
      final ScopeConfigurable scopeConfigurable = (ScopeConfigurable)node.getConfigurable();
      final NamedScope namedScope = scopeConfigurable.getEditableObject();
      if (order.size() <= i) return true;
      final String name = order.get(i);
      if (!Comparing.strEqual(name, namedScope.getName())) return true;
      if (isInitialized(scopeConfigurable)) {
        final NamedScopesHolder holder = scopeConfigurable.getHolder();
        final NamedScope scope = holder.getScope(name);
        if (scope == null) return true;
        if (scopeConfigurable.isModified()) return true;
      }
    }
    return false;
  }

  private void processScopes() {
    final List<NamedScope> localScopes = new ArrayList<>();
    final List<NamedScope> sharedScopes = new ArrayList<>();
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      final MyNode node = (MyNode)myRoot.getChildAt(i);
      final ScopeConfigurable scopeConfigurable = (ScopeConfigurable)node.getConfigurable();
      final NamedScope namedScope = scopeConfigurable.getScope();
      if (scopeConfigurable.getHolder() == myLocalScopesManager) {
        localScopes.add(namedScope);
      }
      else {
        sharedScopes.add(namedScope);
      }
    }
    myLocalScopesManager.setScopes(localScopes.toArray(NamedScope.EMPTY_ARRAY));
    mySharedScopesManager.setScopes(sharedScopes.toArray(NamedScope.EMPTY_ARRAY));
  }

  private void loadStateOrder() {
    final List<String> order = getScopesState().myOrder;
    order.clear();
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      order.add(((MyNode)myRoot.getChildAt(i)).getDisplayName());
    }
  }

  private void loadScopes(final NamedScopesHolder holder) {
    final NamedScope[] scopes = holder.getScopes();
    for (NamedScope scope : scopes) {
      if (isPredefinedScope(scope)) continue;
      myRoot.add(new MyNode(new ScopeConfigurable(scope, holder == mySharedScopesManager, myProject, TREE_UPDATER)));
    }
  }

  private boolean isPredefinedScope(final NamedScope scope) {
    return getPredefinedScopes(myProject).contains(scope);
  }

  private static Collection<NamedScope> getPredefinedScopes(Project project) {
    final Collection<NamedScope> result = new ArrayList<>();
    result.addAll(NamedScopeManager.getInstance(project).getPredefinedScopes());
    result.addAll(DependencyValidationManager.getInstance(project).getPredefinedScopes());
    return result;
  }

  @Override
  protected void initTree() {
    myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        final TreePath path = e.getOldLeadSelectionPath();
        if (path != null) {
          final MyNode node = (MyNode)path.getLastPathComponent();
          final NamedConfigurable namedConfigurable = node.getConfigurable();
          if (namedConfigurable instanceof ScopeConfigurable) {
            ((ScopeConfigurable)namedConfigurable).cancelCurrentProgress();
          }
        }
      }
    });
    super.initTree();
    myTree.setShowsRootHandles(false);
    new TreeSpeedSearch(myTree, treePath -> ((MyNode)treePath.getLastPathComponent()).getDisplayName(), true);

    myTree.getEmptyText().setText(IdeBundle.message("scopes.no.scoped"));
  }

  @Override
  protected boolean wasObjectStored(Object editableObject) {
    if (editableObject instanceof NamedScope) {
      NamedScope scope = (NamedScope)editableObject;
      final String scopeName = scope.getName();
      return myLocalScopesManager.getScope(scopeName) != null || mySharedScopesManager.getScope(scopeName) != null;
    }
    return false;
  }

  @Override
  public String getDisplayName() {
    return IdeBundle.message("scopes.display.name");
  }

  @Override
  @NotNull
  @NonNls
  public String getHelpTopic() {
    return PROJECT_SCOPES;  //todo help id
  }

  @Override
  protected void updateSelection(@Nullable final NamedConfigurable configurable) {
    super.updateSelection(configurable);
    if (configurable instanceof ScopeConfigurable) {
      ((ScopeConfigurable)configurable).restoreCanceledProgress();
    }
  }

  @Override
  protected
  @Nullable
  String getEmptySelectionString() {
    return "Select a scope to view or edit its details here";
  }

  private String createUniqueName() {
    String str = InspectionsBundle.message("inspection.profile.unnamed");
    final HashSet<String> treeScopes = new HashSet<>();
    obtainCurrentScopes(treeScopes);
    if (!treeScopes.contains(str)) return str;
    int i = 1;
    while (true) {
      if (!treeScopes.contains(str + i)) return str + i;
      i++;
    }
  }

  private void obtainCurrentScopes(final HashSet<? super String> scopes) {
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      final MyNode node = (MyNode)myRoot.getChildAt(i);
      final NamedScope scope = (NamedScope)node.getConfigurable().getEditableObject();
      scopes.add(scope.getName());
    }
  }

  private void addNewScope(final NamedScope scope, final boolean isLocal) {
    final MyNode nodeToAdd = new MyNode(new ScopeConfigurable(scope, !isLocal, myProject, TREE_UPDATER));
    myRoot.add(nodeToAdd);
    ((DefaultTreeModel)myTree.getModel()).reload(myRoot);
    selectNodeInTree(nodeToAdd);
  }

  private void createScope(final boolean isLocal, String title, final PackageSet set) {
    NamedScopesHolder holder = isLocal ? myLocalScopesManager : mySharedScopesManager;
    final String newName = Messages.showInputDialog(myTree, IdeBundle.message("add.scope.name.label"), title,
                                                    Messages.getInformationIcon(), createUniqueName(), new InputValidator() {
      @Override
      public boolean checkInput(String inputString) {
        for (NamedScope scope : holder.getPredefinedScopes()) {
          if (Comparing.strEqual(scope.getName(), inputString.trim())) {
            return false;
          }
        }
        return inputString.trim().length() > 0;
      }

      @Override
      public boolean canClose(String inputString) {
        return checkInput(inputString);
      }
    });
    if (newName != null) {
      addNewScope(holder.createScope(newName, set), isLocal);
    }
  }

  @Override
  @NotNull
  @NonNls
  public String getId() {
    return getHelpTopic();
  }

  private class MyAddAction extends ActionGroup implements ActionGroupWithPreselection, DumbAware {

    private AnAction[] myChildren;
    private final boolean myFromPopup;

    MyAddAction(boolean fromPopup) {
      super(IdeBundle.message("add.scope.popup.title"), true);
      myFromPopup = fromPopup;
      final Presentation presentation = getTemplatePresentation();
      presentation.setIcon(IconUtil.getAddIcon());
      registerCustomShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD), myTree);
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      if (myFromPopup) {
        setPopup(false);
      }
    }

    @Override
    @NotNull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      if (myChildren == null) {
        myChildren = new AnAction[2];
        myChildren[0] = new DumbAwareAction(IdeBundle.message("add.local.scope.action.text"), IdeBundle.message("add.local.scope.action.text"),
                                            myLocalScopesManager.getIcon()) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            createScope(true, IdeBundle.message("add.scope.dialog.title"), null);
          }
        };
        myChildren[1] = new DumbAwareAction(IdeBundle.message("add.shared.scope.action.text"), IdeBundle.message("add.shared.scope.action.text"),
                                            mySharedScopesManager.getIcon()) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            createScope(false, IdeBundle.message("add.scope.dialog.title"), null);
          }
        };
      }
      if (myFromPopup) {
        final AnAction action = myChildren[getDefaultIndex()];
        action.getTemplatePresentation().setIcon(IconUtil.getAddIcon());
        return new AnAction[]{action};
      }
      return myChildren;
    }

    @Override
    public ActionGroup getActionGroup() {
      return this;
    }

    @Override
    public int getDefaultIndex() {
      final TreePath selectionPath = myTree.getSelectionPath();
      if (selectionPath != null) {
        final MyNode node = (MyNode)selectionPath.getLastPathComponent();
        Object editableObject = node.getConfigurable().getEditableObject();
        if (editableObject instanceof NamedScope) {
          editableObject = ((MyNode)node.getParent()).getConfigurable().getEditableObject();
        }
        if (editableObject instanceof NamedScopeManager) {
          return 0;
        }
        else if (editableObject instanceof DependencyValidationManager) {
          return 1;
        }
      }
      return 0;
    }
  }

  private class MyMoveAction extends AnAction {
    private final int myDirection;

    protected MyMoveAction(String text, Icon icon, int direction) {
      super(text, text, icon);
      ShortcutSet shortcutSet = direction < 0 ? CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.UP)
                                              : CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.DOWN);
      registerCustomShortcutSet(shortcutSet, myTree);
      myDirection = direction;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      TreeUtil.moveSelectedRow(myTree, myDirection);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      final Presentation presentation = e.getPresentation();
      presentation.setEnabled(false);
      final TreePath selectionPath = myTree.getSelectionPath();
      if (selectionPath != null) {
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
        if (treeNode.getUserObject() instanceof ScopeConfigurable) {
          if (myDirection < 0) {
            presentation.setEnabled(treeNode.getPreviousSibling() != null);
          }
          else {
            presentation.setEnabled(treeNode.getNextSibling() != null);
          }
        }
      }
    }
  }

  private class MyCopyAction extends AnAction {
    MyCopyAction() {
      super(ExecutionBundle.message("copy.configuration.action.name"), ExecutionBundle.message("copy.configuration.action.name"),
            COPY_ICON);
      registerCustomShortcutSet(CommonShortcuts.getDuplicate(), myTree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      NamedScope scope = (NamedScope)getSelectedObject();
      if (scope != null) {
        final ScopeConfigurable configurable =
          (ScopeConfigurable)((MyNode)myTree.getSelectionPath().getLastPathComponent()).getConfigurable();
        NamedScopesHolder holder = configurable.getHolder();
        PackageSet set = scope.getValue();
        addNewScope(holder.createScope(createUniqueName(), set == null ? null : set.createCopy()), holder == myLocalScopesManager);
      }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(getSelectedObject() instanceof NamedScope);
    }
  }

  private class MySaveAsAction extends AnAction {
    MySaveAsAction() {
      super(ExecutionBundle.message("action.name.save.as.configuration"), ExecutionBundle.message("action.name.save.as.configuration"),
            AllIcons.Actions.Menu_saveall);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      final TreePath selectionPath = myTree.getSelectionPath();
      if (selectionPath != null) {
        final MyNode node = (MyNode)selectionPath.getLastPathComponent();
        final NamedConfigurable configurable = node.getConfigurable();
        if (configurable instanceof ScopeConfigurable) {
          final ScopeConfigurable scopeConfigurable = (ScopeConfigurable)configurable;
          PackageSet set = scopeConfigurable.getEditableObject().getValue();
          if (set != null) {
            if (scopeConfigurable.getHolder() == mySharedScopesManager) {
              createScope(true, IdeBundle.message("scopes.save.dialog.title.local"), set.createCopy());
            }
            else {
              createScope(false, IdeBundle.message("scopes.save.dialog.title.shared"), set.createCopy());
            }
          }
        }
      }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(getSelectedObject() instanceof NamedScope);
    }
  }

  public static class ScopeChooserConfigurableState extends MasterDetailsState {
    @XCollection(propertyElementName = "order", elementName = "scope", valueAttributeName = "name")
    public List<String> myOrder = new ArrayList<>();
  }
}
