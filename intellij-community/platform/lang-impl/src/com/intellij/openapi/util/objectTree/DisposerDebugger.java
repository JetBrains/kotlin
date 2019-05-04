// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.util.objectTree;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeStructureBase;
import com.intellij.ide.util.treeView.AlphaComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.debugger.UiDebuggerExtension;
import com.intellij.ui.speedSearch.ElementFilter;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.ui.treeStructure.filtered.FilteringTreeBuilder;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.TextTransferable;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.DefaultCaret;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DisposerDebugger implements UiDebuggerExtension, Disposable  {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.util.objectTree.DisposerDebugger");

  private JComponent myComponent;
  private JBTabs myTreeTabs;

  private void initUi() {
    myComponent = new JPanel();

    myTreeTabs = JBTabsFactory.createTabs(null, this);

    final Splitter splitter = new Splitter(true);

    final JBTabs bottom = JBTabsFactory.createTabs(null, this);
    final AllocationPanel allocations = new AllocationPanel(myTreeTabs);
    bottom.addTab(new TabInfo(allocations).setText("Allocation")).setActions(allocations.getActions(), ActionPlaces.UNKNOWN);


    splitter.setFirstComponent(myTreeTabs.getComponent());
    splitter.setSecondComponent(bottom.getComponent());

    myComponent.setLayout(new BorderLayout());
    myComponent.add(splitter, BorderLayout.CENTER);
    JLabel countLabel = new JLabel("Total disposable count: " + Disposer.getTree().size());
    myComponent.add(countLabel, BorderLayout.SOUTH);

    addTree(new DisposerTree(this), "All", false);
    addTree(new DisposerTree(this), "Watch", true);
  }

  private void addTree(DisposerTree tree, String name, boolean canClear) {
    final DefaultActionGroup group = new DefaultActionGroup();
    if (canClear) {
      group.add(new ClearAction(tree));
    }

    myTreeTabs.addTab(new TabInfo(tree).setText(name).setObject(tree).setActions(group, ActionPlaces.UNKNOWN));
  }

  private static class ClearAction extends AnAction {
    private final DisposerTree myTree;

    private ClearAction(DisposerTree tree) {
      super("Clear");
      myTree = tree;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setIcon(AllIcons.Debugger.Watch);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myTree.clear();
    }
  }

  private static class AllocationPanel extends JPanel implements TreeSelectionListener {

    private final JEditorPane myAllocation;
    private final DefaultActionGroup myActions;

    private final JBTabs myTreeTabs;

    private AllocationPanel(JBTabs treeTabs) {
      myTreeTabs = treeTabs;

      myAllocation = new JEditorPane();
      final DefaultCaret caret = new DefaultCaret();
      myAllocation.setCaret(caret);
      caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
      myAllocation.setEditable(false);

      setLayout(new BorderLayout());
      add(ScrollPaneFactory.createScrollPane(myAllocation), BorderLayout.CENTER);


      treeTabs.addListener(new TabsListener() {
        @Override
        public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
          updateText();
        }

        @Override
        public void beforeSelectionChanged(TabInfo oldSelection, TabInfo newSelection) {
          removeListener(oldSelection);
          addListener(newSelection);
        }
      });

      myActions = new DefaultActionGroup();
      myActions.add(new CopyAllocationAction());
    }

    private void addListener(TabInfo info) {
      if (info == null) return;
      ((DisposerTree)info.getObject()).getTree().getSelectionModel().addTreeSelectionListener(this);
    }

    private void removeListener(TabInfo info) {
      if (info == null) return;
      ((DisposerTree)info.getObject()).getTree().getSelectionModel().removeTreeSelectionListener(this);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
      updateText();
    }

    private void updateText() {
      if (myTreeTabs.getSelectedInfo() == null) return;

      final DisposerTree tree = (DisposerTree)myTreeTabs.getSelectedInfo().getObject();

      final DisposerNode node = tree.getSelectedNode();
      if (node != null) {
        final Throwable allocation = node.getAllocation();
        if (allocation != null) {
          final StringWriter s = new StringWriter();
          final PrintWriter writer = new PrintWriter(s);
          allocation.printStackTrace(writer);
          myAllocation.setText(s.toString());
          return;
        }
      }

      myAllocation.setText(null);
    }

    public ActionGroup getActions() {
      return myActions;
    }

    private class CopyAllocationAction extends AnAction {
      CopyAllocationAction() {
        super("Copy", "Copy allocation to clipboard", PlatformIcons.COPY_ICON);
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(myAllocation.getDocument().getLength() > 0);
      }

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        try {
          CopyPasteManager.getInstance().setContents(new TextTransferable(StringUtil.notNullize(myAllocation.getText())));
        }
        catch (HeadlessException e1) {
          LOG.error(e1);
        }
      }
    }
  }

  private static class DisposerTree extends JPanel implements Disposable, ObjectTreeListener, ElementFilter<DisposerNode> {

    private final FilteringTreeBuilder myTreeBuilder;
    private final Tree myTree;
    private long myModificationToFilter;

    private DisposerTree(Disposable parent) {
      myModificationToFilter = -1;

      final DisposerStructure structure = new DisposerStructure(this);
      final DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode());
      final Tree tree = new Tree(model);
      tree.setRootVisible(false);
      tree.setShowsRootHandles(true);
      tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      myTreeBuilder = new FilteringTreeBuilder(tree, this, structure, AlphaComparator.INSTANCE) {
        @Override
        public boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
          return structure.getRootElement() == getOriginalNode(nodeDescriptor);
        }
      };
      myTreeBuilder.setFilteringMerge(200, MergingUpdateQueue.ANY_COMPONENT);
      Disposer.register(this, myTreeBuilder);
      myTree = tree;

      setLayout(new BorderLayout());
      add(ScrollPaneFactory.createScrollPane(myTree), BorderLayout.CENTER);

      Disposer.getTree().addListener(this);

      Disposer.register(parent, this);
    }

    @Override
    public boolean shouldBeShowing(DisposerNode value) {
      return value.getValue().getModification() > myModificationToFilter;
    }

    @Override
    public void objectRegistered(@NotNull Object node) {
      queueUpdate();
    }



    @Override
    public void objectExecuted(@NotNull Object node) {
      queueUpdate();
    }

    private void queueUpdate() {
      UIUtil.invokeLaterIfNeeded(() -> myTreeBuilder.refilterAsync());
    }

    @Override
    public void dispose() {
      Disposer.getTree().removeListener(this);
    }

    @Nullable
    public DisposerNode getSelectedNode() {
      final Set<DisposerNode> nodes = myTreeBuilder.getSelectedElements(DisposerNode.class);
      return nodes.size() == 1 ? nodes.iterator().next() : null;
    }

    public Tree getTree() {
      return myTree;
    }

    public void clear() {
      myModificationToFilter = Disposer.getTree().getModification();
      myTreeBuilder.refilterAsync();
    }
  }

  @Override
  public JComponent getComponent() {
    if (myComponent == null) {
      initUi();
    }

    return myComponent;
  }

  @Override
  public String getName() {
    return "Disposer";
  }

  @Override
  public void dispose() {
  }


  private static final ObjectNode<Disposable> ROOT = new ObjectNode<>(Disposer.getTree(), null, Disposer.newDisposable(), -1);
  private static class DisposerStructure extends AbstractTreeStructureBase {
    private final DisposerNode myRoot;

    private DisposerStructure(DisposerTree tree) {
      super(null);
      myRoot = new DisposerNode(tree, ROOT);
    }

    @Override
    public List<TreeStructureProvider> getProviders() {
      return null;
    }

    @NotNull
    @Override
    public Object getRootElement() {
      return myRoot;
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
      return false;
    }
  }

  private static class DisposerNode extends AbstractTreeNode<ObjectNode> {
    private final DisposerTree myTree;

    private DisposerNode(DisposerTree tree, @NotNull ObjectNode value) {
      super(null, value);
      myTree = tree;
    }

    @Override
    @NotNull
    public Collection<? extends AbstractTreeNode> getChildren() {
      final ObjectNode value = getValue();
      if (value != ROOT) {
        Collection<ObjectNode> subnodes = value.getChildren();
        List<DisposerNode> children = new ArrayList<>(subnodes.size());
        for (ObjectNode subnode : subnodes) {
          children.add(new DisposerNode(myTree, subnode));
        }
        return children;
      }
      else {
        final ObjectTree<Disposable> tree = Disposer.getTree();
        final Set<Disposable> root = tree.getRootObjects();
        ArrayList<DisposerNode> children = new ArrayList<>(root.size());
        for (Disposable each : root) {
          children.add(new DisposerNode(myTree, tree.getNode(each)));
        }
        return children;
      }
    }

    @Nullable
    public Throwable getAllocation() {
      return getValue() != null ? getValue().getTrace() : null;
    }

    @Override
    protected boolean shouldUpdateData() {
      return true;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      if (getValue() != null) {
        final Object object = getValue().getObject();
        final String classString = object.getClass().toString();
        final String objectString = object.toString();

        presentation.setPresentableText(objectString);

        if (getValue().getOwnModification() < myTree.myModificationToFilter) {
          presentation.setForcedTextForeground(JBColor.GRAY);
        }

        if (objectString != null) {
          final int dogIndex = objectString.lastIndexOf("@");
          if (dogIndex >= 0) {
            final String fqNameObject = objectString.substring(0, dogIndex);
            final String fqNameClass = classString.substring("class ".length());
            if (fqNameObject.equals(fqNameClass)) return;
          }
        }

        presentation.setLocationString(classString);
      }
    }
  }

  @Override
  public void disposeUiResources() {
    myComponent = null;
  }
}
