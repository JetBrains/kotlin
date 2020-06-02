// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.moduleDependencies;

import com.intellij.CommonBundle;
import com.intellij.ProjectTopics;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.NavigatableWithText;
import com.intellij.ui.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.graph.DFSTBuilder;
import com.intellij.util.graph.Graph;
import com.intellij.util.graph.GraphAlgorithms;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author anna
 */
public class ModulesDependenciesPanel extends JPanel implements Disposable {
  public static final String HELP_ID = "module.dependencies.tool.window";

  private static final Comparator<DefaultMutableTreeNode> NODE_COMPARATOR = (o1, o2) -> {
    if (!(o1.getUserObject() instanceof MyUserObject)) return 1;
    if (!(o2.getUserObject() instanceof MyUserObject)) return -1;
    return o1.getUserObject().toString().compareToIgnoreCase(o2.getUserObject().toString());
  };

  private static final ColoredTreeCellRenderer NODE_RENDERER = new ColoredTreeCellRenderer() {
    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
      if (userObject instanceof MyUserObject) {
        MyUserObject node = (MyUserObject)userObject;
        setIcon(ModuleType.get(node.myModule).getIcon());
        append(node.myModule.getName(), node.myInCycle ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
      else if (userObject != null) {
        append(userObject.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      }
    }
  };

  private final Project myProject;
  private final Module[] myModules;
  private final DependenciesAnalyzeManager.State myState;

  private final Tree myLeftTree;
  private final Tree myRightTree;
  private final Splitter mySplitter;
  private final JTextComponent myPathField;

  private Content myContent;
  private Graph<Module> myModuleGraph;
  private final Map<Module, Boolean> myCycleMap = new HashMap<>();

  public ModulesDependenciesPanel(@NotNull Project project, Module @Nullable [] modules) {
    super(new BorderLayout());

    myProject = project;
    myModules = modules != null ? modules : ModuleManager.getInstance(project).getModules();
    myState = DependenciesAnalyzeManager.getInstance(project).getState();
    updateModuleGraph();

    myLeftTree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode("Root")));
    installLeftTreeListeners();
    installTreeActions(myLeftTree, false);

    myRightTree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode("Root")));
    installTreeActions(myRightTree, true);

    mySplitter = new Splitter();
    mySplitter.setFirstComponent(new MyTreePanel(myLeftTree, myProject));
    mySplitter.setSecondComponent(new MyTreePanel(myRightTree, myProject));
    add(mySplitter, BorderLayout.CENTER);

    myPathField = new JTextField();
    myPathField.setEditable(false);
    add(createNorthPanel(), BorderLayout.NORTH);

    project.getMessageBus().connect(this).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        updateModuleGraph();
        updateSplitterProportion();
        updateLeftTree();
      }
    });

    updateSplitterProportion();
    updateLeftTree();
  }

  private void updateModuleGraph() {
    myModuleGraph = ModuleManager.getInstance(myProject).moduleGraph(myState.includeTests);
    myCycleMap.clear();
  }

  private Iterable<Module> getModuleDependencies(Module module) {
    final Iterator<Module> iterator = myState.forwardDirection ? myModuleGraph.getIn(module) : myModuleGraph.getOut(module);
    return new Iterable<Module>() {
      @Override
      public Iterator<Module> iterator() {
        return iterator;
      }
    };
  }

  private boolean isInCycle(Module module) {
    Boolean inCycle = myCycleMap.get(module);
    if (inCycle == null) {
      Set<List<Module>> cycles = GraphAlgorithms.getInstance().findCycles(myModuleGraph, module);
      inCycle = !cycles.isEmpty();
      myCycleMap.put(module, inCycle);
      for (List<Module> cycle : cycles) {
        for (Module moduleInCycle : cycle) {
          myCycleMap.put(moduleInCycle, true);
        }
      }
    }
    return inCycle;
  }

  private void installLeftTreeListeners() {
    myLeftTree.addTreeExpansionListener(new TreeExpansionListener() {
      @Override
      public void treeCollapsed(TreeExpansionEvent event) { }

      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        DefaultMutableTreeNode expandedNode = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
        for (int i = 0; i < expandedNode.getChildCount(); i++) {
          DefaultMutableTreeNode child = (DefaultMutableTreeNode)expandedNode.getChildAt(i);
          if (child.getChildCount() == 0 && !isLooped(event.getPath(), child)) {
            Module module = ((MyUserObject)child.getUserObject()).myModule;
            for (Module dependency : getModuleDependencies(module)) {
              child.add(new DefaultMutableTreeNode(new MyUserObject(isInCycle(dependency), dependency)));
            }
            TreeUtil.sortRecursively(child, NODE_COMPARATOR);
          }
        }
      }

      private boolean isLooped(TreePath path, DefaultMutableTreeNode child) {
        for (Object o : path.getPath()) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)o;
          if (node != child && Comparing.equal(node.getUserObject(), child.getUserObject())) {
            return true;
          }
        }
        return false;
      }
    });

    myLeftTree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        TreePath selectionPath = myLeftTree.getSelectionPath();
        if (selectionPath != null) {
          myPathField.setText(StringUtil.join(selectionPath.getPath(), o -> {
            Object userObject = ((DefaultMutableTreeNode)o).getUserObject();
            return userObject instanceof MyUserObject ? ((MyUserObject)userObject).myModule.getName() : "";
          }, " : "));

          DefaultMutableTreeNode selection = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
          if (selection != null) {
            updateRightTree(((MyUserObject)selection.getUserObject()).myModule);
          }
        }
      }
    });
  }

  private static void installTreeActions(Tree tree, boolean enableExpandAll) {
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setCellRenderer(NODE_RENDERER);
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);

    TreeUtil.installActions(tree);

    new TreeSpeedSearch(tree, o -> o.getLastPathComponent().toString(), true);

    DefaultActionGroup group = new DefaultActionGroup();
    CommonActionsManager commonActionManager = CommonActionsManager.getInstance();
    ActionManager globalActionManager = ActionManager.getInstance();

    TreeExpander treeExpander = new MyTreeExpander(tree, enableExpandAll);
    group.add(commonActionManager.createExpandAllAction(treeExpander, tree));
    group.add(commonActionManager.createCollapseAllAction(treeExpander, tree));
    group.add(globalActionManager.getAction(IdeActions.ACTION_EDIT_SOURCE));
    group.add(Separator.getInstance());
    group.add(globalActionManager.getAction(IdeActions.ACTION_ANALYZE_DEPENDENCIES));
    group.add(globalActionManager.getAction(IdeActions.ACTION_ANALYZE_BACK_DEPENDENCIES));
    group.add(globalActionManager.getAction(IdeActions.ACTION_ANALYZE_CYCLIC_DEPENDENCIES));
    group.add(globalActionManager.getAction(IdeActions.ACTION_ANALYZE_MODULE_DEPENDENCIES));

    PopupHandler.installUnknownPopupHandler(tree, group, ActionManager.getInstance());
  }

  private void updateSplitterProportion() {
    DFSTBuilder<Module> builder = new DFSTBuilder<>(myModuleGraph);
    mySplitter.setProportion(builder.isAcyclic() ? 1.0f : 0.6f);
  }

  private JComponent createNorthPanel() {
    DefaultActionGroup group = new DefaultActionGroup();

    group.add(new AnAction(CommonBundle.messagePointer("action.close"), AllIcons.Actions.Cancel) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        DependenciesAnalyzeManager.getInstance(myProject).closeContent(myContent);
      }
    });

    final AnAction analyzeDepsAction = ActionManager.getInstance().getAction(IdeActions.ACTION_ANALYZE_DEPENDENCIES);
    group.add(new AnAction(analyzeDepsAction.getTemplatePresentation().getText(), null, AllIcons.Toolwindows.ToolWindowInspection) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        analyzeDepsAction.actionPerformed(e);
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        analyzeDepsAction.update(e);
      }
    });

    group.add(new ToggleAction(CodeInsightBundle.message("action.module.dependencies.direction")) {
      @Override
      public boolean isSelected(@NotNull AnActionEvent e) {
        return !myState.forwardDirection;
      }

      @Override
      public void setSelected(@NotNull AnActionEvent e, boolean state) {
        myState.forwardDirection = !state;
        updateLeftTree();
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setIcon(myState.forwardDirection ? AllIcons.Hierarchy.Subtypes : AllIcons.Hierarchy.Supertypes);
        super.update(e);
      }
    });

    group.add(new ToggleAction(CodeInsightBundle.message("action.module.dependencies.tests"), null, AllIcons.Nodes.TestSourceFolder) {
      @Override
      public boolean isSelected(@NotNull AnActionEvent e) {
        return myState.includeTests;
      }

      @Override
      public void setSelected(@NotNull AnActionEvent e, boolean state) {
        myState.includeTests = state;
        updateModuleGraph();
        updateLeftTree();
      }
    });

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("ModulesDependencies", group, true);
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(toolbar.getComponent(), BorderLayout.WEST);
    panel.add(myPathField, BorderLayout.CENTER);
    return panel;
  }

  private void updateLeftTree() {
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode)myLeftTree.getModel().getRoot();
    root.removeAllChildren();

    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      for (Module module : myModules) {
        if (module.isDisposed()) continue;
        ProgressManager.progress(CodeInsightBundle.message("update.module.tree.progress.text", module.getName()));

        DefaultMutableTreeNode moduleNode = new DefaultMutableTreeNode(new MyUserObject(isInCycle(module), module));
        root.add(moduleNode);
        for (Module dependency : getModuleDependencies(module)) {
          moduleNode.add(new DefaultMutableTreeNode(new MyUserObject(isInCycle(dependency), dependency)));
        }
      }
    }, CodeInsightBundle.message("update.module.tree.progress.title"), true, myProject);

    TreeUtil.sortRecursively(root, NODE_COMPARATOR);
    ((DefaultTreeModel)myLeftTree.getModel()).reload();
    TreeUtil.promiseSelectFirst(myLeftTree);
  }

  private void updateRightTree(Module module) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode)myRightTree.getModel().getRoot();
    root.removeAllChildren();

    Set<List<Module>> cycles = GraphAlgorithms.getInstance().findCycles(myModuleGraph, module);
    int index = 1;
    for (List<Module> modules : cycles) {
      DefaultMutableTreeNode cycle = new DefaultMutableTreeNode(CodeInsightBundle.message("module.dependencies.cycle.node.text", index++));
      root.add(cycle);
      cycle.add(new DefaultMutableTreeNode(new MyUserObject(false, module)));
      for (Module moduleInCycle : modules) {
        cycle.add(new DefaultMutableTreeNode(new MyUserObject(false, moduleInCycle)));
      }
    }

    ((DefaultTreeModel)myRightTree.getModel()).reload();
    TreeUtil.expandAll(myRightTree);
  }

  public void setContent(Content content) {
    myContent = content;
  }

  @Override
  public void dispose() { }


  private static class MyUserObject implements NavigatableWithText {
    private final Module myModule;
    private final boolean myInCycle;

    MyUserObject(boolean inCycle, Module module) {
      myInCycle = inCycle;
      myModule = module;
    }

    @Override
    public void navigate(boolean requestFocus) {
      ProjectSettingsService.getInstance(myModule.getProject()).openModuleSettings(myModule);
    }

    @Override
    public boolean canNavigate() {
      return !myModule.isDisposed();
    }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }

    @Override
    public String getNavigateActionText(boolean focusEditor) {
      return ActionsBundle.message("action.ModuleSettings.navigate");
    }

    @Override
    public boolean equals(Object o) {
      return o == this || o instanceof MyUserObject && myModule.equals(((MyUserObject)o).myModule);
    }

    @Override
    public int hashCode() {
      return myModule.hashCode();
    }

    @Override
    public String toString() {
      return myModule.getName();
    }
  }

  private static class MyTreePanel extends JPanel implements DataProvider {
    private final Tree myTree;
    private final Project myProject;

    MyTreePanel(Tree tree, Project project) {
      super(new BorderLayout());
      myTree = tree;
      myProject = project;
      add(ScrollPaneFactory.createScrollPane(myTree), BorderLayout.CENTER);
    }

    @Override
    public Object getData(@NotNull String dataId) {
      if (CommonDataKeys.PROJECT.is(dataId)) {
        return myProject;
      }
      if (LangDataKeys.MODULE_CONTEXT.is(dataId)) {
        TreePath selectionPath = myTree.getLeadSelectionPath();
        if (selectionPath != null && selectionPath.getLastPathComponent() instanceof DefaultMutableTreeNode) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
          if (node.getUserObject() instanceof MyUserObject) {
            return ((MyUserObject)node.getUserObject()).myModule;
          }
        }
      }
      if (PlatformDataKeys.HELP_ID.is(dataId)) {
        return HELP_ID;
      }
      if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
        TreePath selectionPath = myTree.getLeadSelectionPath();
        if (selectionPath != null && selectionPath.getLastPathComponent() instanceof DefaultMutableTreeNode) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
          if (node.getUserObject() instanceof MyUserObject) {
            return node.getUserObject();
          }
        }
      }
      return null;
    }
  }

  private static class MyTreeExpander extends DefaultTreeExpander {
    private final boolean myEnableExpandAll;

    MyTreeExpander(Tree tree, boolean enableExpandAll) {
      super(tree);
      myEnableExpandAll = enableExpandAll;
    }

    @Override
    public boolean canExpand() {
      return myEnableExpandAll && super.canExpand();
    }

    @Override
    protected void collapseAll(@NotNull JTree tree, int keepSelectionLevel) {
      super.collapseAll(tree, 3);
    }
  }
}