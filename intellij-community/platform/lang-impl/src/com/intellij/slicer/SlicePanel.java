// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.slicer;

import com.intellij.icons.AllIcons;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.RefreshAction;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.pom.Navigatable;
import com.intellij.ui.*;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageViewPresentation;
import com.intellij.usages.UsageViewSettings;
import com.intellij.usages.impl.UsagePreviewPanel;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

public abstract class SlicePanel extends JPanel implements TypeSafeDataProvider, Disposable {
  private final SliceTreeBuilder myBuilder;
  private final JTree myTree;

  private final AutoScrollToSourceHandler myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
    @Override
    protected boolean isAutoScrollMode() {
      return isAutoScroll();
    }

    @Override
    protected void setAutoScrollMode(final boolean state) {
      setAutoScroll(state);
    }
  };
  private UsagePreviewPanel myUsagePreviewPanel;
  private final Project myProject;
  private boolean isDisposed;
  private final ToolWindow myToolWindow;
  private final SliceLanguageSupportProvider myProvider;

  protected SlicePanel(@NotNull final Project project,
                       boolean dataFlowToThis,
                       @NotNull SliceNode rootNode,
                       boolean splitByLeafExpressions,
                       @NotNull final ToolWindow toolWindow) {
    super(new BorderLayout());
    myProvider = rootNode.getProvider();
    myToolWindow = toolWindow;
    ApplicationManager.getApplication().assertIsDispatchThread();
    myProject = project;

    myProject.getMessageBus().connect(this).subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
      ToolWindowAnchor myAnchor = toolWindow.getAnchor();

      @Override
      public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        if (!project.isOpen()) {
          return;
        }
        if (toolWindow.getAnchor() != myAnchor) {
          myAnchor = myToolWindow.getAnchor();
          layoutPanel();
        }
      }
    });

    myTree = createTree();

    myBuilder = new SliceTreeBuilder(myTree, project, dataFlowToThis, rootNode, splitByLeafExpressions);
    myBuilder.setCanYieldUpdate(!ApplicationManager.getApplication().isUnitTestMode());

    Disposer.register(this, myBuilder);

    myBuilder.addSubtreeToUpdate((DefaultMutableTreeNode)myTree.getModel().getRoot(), () -> {
      if (isDisposed || myBuilder.isDisposed() || myProject.isDisposed()) return;
      final SliceNode rootNode1 = myBuilder.getRootSliceNode();
      myBuilder.expand(rootNode1, new Runnable() {
        @Override
        public void run() {
          if (isDisposed || myBuilder.isDisposed() || myProject.isDisposed()) return;
          myBuilder.select(rootNode1.myCachedChildren.get(0)); //first there is ony one child
        }
      });
      treeSelectionChanged();
    });

    layoutPanel();
  }

  private void layoutPanel() {
    if (myUsagePreviewPanel != null) {
      Disposer.dispose(myUsagePreviewPanel);
    }
    removeAll();
    JScrollPane pane = ScrollPaneFactory.createScrollPane(myTree);

    if (isPreview()) {
      pane.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT | SideBorder.RIGHT));

      boolean vertical = myToolWindow.getAnchor() == ToolWindowAnchor.LEFT || myToolWindow.getAnchor() == ToolWindowAnchor.RIGHT;
      Splitter splitter = new Splitter(vertical, UsageViewSettings.getInstance().getPreviewUsagesSplitterProportion());
      splitter.setFirstComponent(pane);
      myUsagePreviewPanel = new UsagePreviewPanel(myProject, new UsageViewPresentation());
      myUsagePreviewPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));

      Disposer.register(this, myUsagePreviewPanel);
      splitter.setSecondComponent(myUsagePreviewPanel);
      add(splitter, BorderLayout.CENTER);
    }
    else {
      pane.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));
      add(pane, BorderLayout.CENTER);
    }

    add(createToolbar().getComponent(), BorderLayout.WEST);

    myTree.getParent().setBackground(UIUtil.getTreeBackground());

    revalidate();
  }

  @Override
  public void dispose() {
    if (myUsagePreviewPanel != null) {
      UsageViewSettings.getInstance().setPreviewUsagesSplitterProportion(((Splitter)myUsagePreviewPanel.getParent()).getProportion());
      myUsagePreviewPanel = null;
    }

    isDisposed = true;
    ToolTipManager.sharedInstance().unregisterComponent(myTree);
  }

  static class MultiLanguageTreeCellRenderer implements TreeCellRenderer {
    @NotNull
    private final SliceUsageCellRendererBase rootRenderer;

    @NotNull
    private final Map<SliceLanguageSupportProvider, SliceUsageCellRendererBase> providersToRenderers = new HashMap<>();

    MultiLanguageTreeCellRenderer(@NotNull SliceUsageCellRendererBase rootRenderer) {
      this.rootRenderer = rootRenderer;
      rootRenderer.setOpaque(false);
    }

    @NotNull
    private SliceUsageCellRendererBase getRenderer(Object value) {
      if (!(value instanceof DefaultMutableTreeNode)) return rootRenderer;

      Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
      if (!(userObject instanceof SliceNode)) return rootRenderer;

      SliceLanguageSupportProvider provider = ((SliceNode)userObject).getProvider();
      if (provider == null) return rootRenderer;

      SliceUsageCellRendererBase renderer = providersToRenderers.get(provider);
      if (renderer == null) {
        renderer = provider.getRenderer();
        renderer.setOpaque(false);
        providersToRenderers.put(provider, renderer);
      }
      return renderer;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
      return getRenderer(value).getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }
  }

  @NotNull
  private JTree createTree() {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    final Tree tree = new Tree(new DefaultTreeModel(root))/* {
      @Override
      protected void paintComponent(Graphics g) {
        DuplicateNodeRenderer.paintDuplicateNodesBackground(g, this);
        super.paintComponent(g);
      }
    }*/;
    tree.setOpaque(false);

    tree.setToggleClickCount(-1);
    tree.setCellRenderer(new MultiLanguageTreeCellRenderer(myProvider.getRenderer()));
    tree.setRootVisible(false);

    tree.setShowsRootHandles(true);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setSelectionPath(new TreePath(root.getPath()));
    //ActionGroup group = (ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_METHOD_HIERARCHY_POPUP);
    //PopupHandler.installPopupHandler(tree, group, ActionPlaces.METHOD_HIERARCHY_VIEW_POPUP, ActionManager.getInstance());
    EditSourceOnDoubleClickHandler.install(tree);

    new TreeSpeedSearch(tree);
    TreeUtil.installActions(tree);
    ToolTipManager.sharedInstance().registerComponent(tree);

    myAutoScrollToSourceHandler.install(tree);

    tree.getSelectionModel().addTreeSelectionListener(e -> treeSelectionChanged());

    tree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (KeyEvent.VK_ENTER == e.getKeyCode()) {
          List<Navigatable> navigatables = getNavigatables();
          if (navigatables.isEmpty()) return;
          for (Navigatable navigatable : navigatables) {
            if (navigatable instanceof AbstractTreeNode && ((AbstractTreeNode)navigatable).getValue() instanceof Usage) {
              navigatable = (Usage)((AbstractTreeNode)navigatable).getValue();
            }
            if (navigatable.canNavigateToSource()) {
              navigatable.navigate(false);
              if (navigatable instanceof Usage) {
                ((Usage)navigatable).highlightInEditor();
              }
            }
          }
          e.consume();
        }
      }
    });

    tree.addTreeWillExpandListener(new TreeWillExpandListener() {
      @Override
      public void treeWillCollapse(TreeExpansionEvent event) {
      }

      @Override
      public void treeWillExpand(TreeExpansionEvent event) {
        TreePath path = event.getPath();
        SliceNode node = fromPath(path);
        node.calculateDupNode();
      }
    });

    return tree;
  }

  private void treeSelectionChanged() {
    SwingUtilities.invokeLater(() -> {
      if (isDisposed) return;
      List<UsageInfo> infos = getSelectedUsageInfos();
      if (infos != null && myUsagePreviewPanel != null) {
        myUsagePreviewPanel.updateLayout(infos);
      }
    });
  }

  private static SliceNode fromPath(TreePath path) {
    Object lastPathComponent = path.getLastPathComponent();
    if (lastPathComponent instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)lastPathComponent;
      Object userObject = node.getUserObject();
      if (userObject instanceof SliceNode) {
        return (SliceNode)userObject;
      }
    }
   return null;
  }

  @Nullable
  private List<UsageInfo> getSelectedUsageInfos() {
    TreePath[] paths = myTree.getSelectionPaths();
    if (paths == null) return null;
    final ArrayList<UsageInfo> result = new ArrayList<>();
    for (TreePath path : paths) {
      SliceNode sliceNode = fromPath(path);
      if (sliceNode != null) {
        result.add(sliceNode.getValue().getUsageInfo());
      }
    }
    if (result.isEmpty()) return null;
    return result;
  }

  @Override
  public void calcData(@NotNull DataKey key, @NotNull DataSink sink) {
    if (key == CommonDataKeys.NAVIGATABLE_ARRAY) {
      List<Navigatable> navigatables = getNavigatables();
      if (!navigatables.isEmpty()) {
        sink.put(CommonDataKeys.NAVIGATABLE_ARRAY, navigatables.toArray(new Navigatable[0]));
      }
    }
  }

  @NotNull
  private List<Navigatable> getNavigatables() {
    TreePath[] paths = myTree.getSelectionPaths();
    if (paths == null) return Collections.emptyList();
    final ArrayList<Navigatable> navigatables = new ArrayList<>();
    for (TreePath path : paths) {
      Object lastPathComponent = path.getLastPathComponent();
      if (lastPathComponent instanceof DefaultMutableTreeNode) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)lastPathComponent;
        Object userObject = node.getUserObject();
        if (userObject instanceof Navigatable) {
          navigatables.add((Navigatable)userObject);
        }
        else if (node instanceof Navigatable) {
          navigatables.add((Navigatable)node);
        }
      }
    }
    return navigatables;
  }

  @NotNull
  private ActionToolbar createToolbar() {
    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(new MyRefreshAction(myTree));
    if (isToShowAutoScrollButton()) {
      actionGroup.add(myAutoScrollToSourceHandler.createToggleAction());
    }

    if (isToShowPreviewButton()) {
      actionGroup.add(new ToggleAction(UsageViewBundle.message("preview.usages.action.text", "usages"),
                                       LangBundle.message("action.preview.description"), AllIcons.Actions.PreviewDetails) {
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
          return isPreview();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
          setPreview(state);
          layoutPanel();
        }
      });
    }

    myProvider.registerExtraPanelActions(actionGroup, myBuilder);
    actionGroup.add(CommonActionsManager.getInstance().createExportToTextFileAction(new SliceToTextFileExporter(myBuilder, UsageViewSettings.getInstance())));

    //actionGroup.add(new ContextHelpAction(HELP_ID));

    return ActionManager.getInstance().createActionToolbar(ActionPlaces.TYPE_HIERARCHY_VIEW_TOOLBAR, actionGroup, false);
  }

  public boolean isToShowAutoScrollButton() {return true;}
  public abstract boolean isAutoScroll();

  public abstract void setAutoScroll(boolean autoScroll);

  public boolean isToShowPreviewButton() {return true;}
  public abstract boolean isPreview();

  public abstract void setPreview(boolean preview);

  protected void close() {
    final ProgressIndicator progress = myBuilder.getUi().getProgress();
    if (progress != null) {
      progress.cancel();
    }
  }

  private final class MyRefreshAction extends RefreshAction {
    private MyRefreshAction(JComponent tree) {
      super(IdeBundle.message("action.refresh"), IdeBundle.message("action.refresh"), AllIcons.Actions.Refresh);
      registerShortcutOn(tree);
    }

    @Override
    public final void actionPerformed(@NotNull final AnActionEvent e) {
      SliceNode rootNode = (SliceNode)myBuilder.getRootNode().getUserObject();
      rootNode.setChanged();
      myBuilder.addSubtreeToUpdate(myBuilder.getRootNode());
    }

    @Override
    public final void update(@NotNull final AnActionEvent event) {
      final Presentation presentation = event.getPresentation();
      presentation.setEnabled(true);
    }
  }

  @TestOnly
  public SliceTreeBuilder getBuilder() {
    return myBuilder;
  }
}
