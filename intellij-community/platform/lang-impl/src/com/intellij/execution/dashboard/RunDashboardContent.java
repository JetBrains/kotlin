/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.execution.dashboard;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.actions.StopAction;
import com.intellij.execution.dashboard.actions.RunDashboardFilterActionGroup;
import com.intellij.execution.dashboard.tree.RunDashboardGrouper;
import com.intellij.execution.dashboard.tree.RunDashboardTreeCellRenderer;
import com.intellij.execution.dashboard.tree.RunDashboardTreeModel;
import com.intellij.execution.dashboard.tree.RunDashboardTreeStructure;
import com.intellij.execution.runners.FakeRerunAction;
import com.intellij.execution.services.ServiceViewTreeLinkMouseListener;
import com.intellij.execution.ui.RunContentManagerImpl;
import com.intellij.execution.ui.layout.impl.RunnerLayoutUiImpl;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DataManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import com.intellij.ide.impl.DataManagerImpl;
import com.intellij.ide.util.treeView.*;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiElement;
import com.intellij.ui.*;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.ui.content.*;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PsiNavigateUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeModelAdapter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

import static com.intellij.execution.dashboard.RunDashboardManagerImpl.getRunnerLayoutUi;
import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;

/**
 * @author konstantin.aleev
 */
public class RunDashboardContent extends JPanel implements TreeContent, Disposable {
  public static final DataKey<RunDashboardContent> KEY = DataKey.create("runDashboardContent");
  @NonNls private static final String PLACE_TOOLBAR = "RunDashboardContent#Toolbar";
  @NonNls static final String RUN_DASHBOARD_CONTENT_TOOLBAR = "RunDashboardContentToolbar";
  @NonNls static final String RUN_DASHBOARD_TREE_TOOLBAR = "RunDashboardTreeToolbar";
  @NonNls private static final String RUN_DASHBOARD_POPUP = "RunDashboardPopup";

  private static final String MESSAGE_CARD = "message";
  private static final String CONTENT_CARD = "content";

  private final Splitter mySplitter;
  private final JPanel myTreePanel;
  private final Tree myTree;
  private final CardLayout myDetailsPanelLayout;
  private final JPanel myDetailsPanel;
  private final JBPanelWithEmptyText myMessagePanel;
  private final JComponent myToolbar;

  private final RunDashboardTreeModel myTreeModel;
  private AbstractTreeBuilder myBuilder;
  private AbstractTreeNode<?> myLastSelection;
  private final Set<Object> myCollapsedTreeNodeValues = new HashSet<>();
  private final List<? extends RunDashboardGrouper> myGroupers;

  @NotNull private final ContentManager myContentManager;
  @NotNull private final ContentManagerListener myContentManagerListener;

  @NotNull private final Project myProject;

  private final DefaultActionGroup myContentActionGroup = new DefaultActionGroup();
  private final DefaultActionGroup myDashboardContentActions = new DefaultActionGroup();
  private final Map<Content, List<AnAction>> myContentActions = new WeakHashMap<>();

  public RunDashboardContent(@NotNull Project project, @NotNull ContentManager contentManager, @NotNull List<? extends RunDashboardGrouper> groupers) {
    super(new BorderLayout());
    myProject = project;
    myGroupers = groupers;

    myTree = new Tree();
    myTreeModel = new RunDashboardTreeModel(new DefaultMutableTreeNode(), myProject, myTree);
    myTree.setModel(myTreeModel);
    myTree.setRootVisible(false);

    myTree.setShowsRootHandles(true);

    myTree.setCellRenderer(new RunDashboardTreeCellRenderer());
    ServiceViewTreeLinkMouseListener mouseListener = new ServiceViewTreeLinkMouseListener(myTree);
    mouseListener.installOn(myTree);
    RowsDnDSupport.install(myTree, myTreeModel);
    UIUtil.putClientProperty(myTree, ANIMATION_IN_RENDERER_ALLOWED, true);

    final RunDashboardManager dashboardManager = RunDashboardManager.getInstance(myProject);

    mySplitter = new OnePixelSplitter(false, dashboardManager.getContentProportion());
    myTreePanel = new JPanel(new BorderLayout());
    myTreePanel.add(ScrollPaneFactory.createScrollPane(myTree, SideBorder.LEFT), BorderLayout.CENTER);
    mySplitter.setFirstComponent(myTreePanel);
    myDetailsPanelLayout = new CardLayout();
    myDetailsPanel = new JPanel(myDetailsPanelLayout);
    myMessagePanel = new JBPanelWithEmptyText().withEmptyText(ExecutionBundle.message("run.dashboard.empty.selection.message"));
    myDetailsPanel.add(MESSAGE_CARD, myMessagePanel);
    mySplitter.setSecondComponent(myDetailsPanel);
    add(mySplitter, BorderLayout.CENTER);

    myToolbar = createToolbar();
    add(myToolbar, BorderLayout.WEST);
    JComponent treeToolbar = createTreeToolBar();
    myTreePanel.add(treeToolbar, BorderLayout.NORTH);

    myContentManager = contentManager;
    myContentManagerListener = new ContentManagerAdapter() {
      @Override
      public void contentAdded(@NotNull ContentManagerEvent event) {
        onContentAdded(event.getContent());
      }

      @Override
      public void contentRemoved(@NotNull ContentManagerEvent event) {
        Content content = event.getContent();
        myContentActions.remove(content);
        updateContentToolbar(myContentManager.getSelectedContent());
      }

      @Override
      public void selectionChanged(@NotNull final ContentManagerEvent event) {
        if (ContentManagerEvent.ContentOperation.add != event.getOperation()) {
          return;
        }
        Content content = event.getContent();
        onContentAdded(content);
        onContentSelectionChanged(content);
      }
    };
    myContentManager.addContentManagerListener(myContentManagerListener);
    myDetailsPanel.add(CONTENT_CARD, myContentManager.getComponent());

    setupBuilder();

    myTree.addTreeSelectionListener(e -> onSelectionChanged());
    myTree.addTreeExpansionListener(new TreeExpansionListener() {
      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        Object value = getNodeValue(event);
        if (value != null) {
          myCollapsedTreeNodeValues.remove(value);
        }
      }

      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        Object value = getNodeValue(event);
        if (value != null) {
          myCollapsedTreeNodeValues.add(value);
        }
      }

      private Object getNodeValue(TreeExpansionEvent event) {
        DefaultMutableTreeNode treeNode = ObjectUtils.tryCast(event.getPath().getLastPathComponent(), DefaultMutableTreeNode.class);
        if (treeNode == null) {
          return null;
        }
        AbstractTreeNode nodeDescriptor = ObjectUtils.tryCast(treeNode.getUserObject(), AbstractTreeNode.class);
        if (nodeDescriptor == null) {
          return null;
        }
        return nodeDescriptor.getValue();
      }
    });
    putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, (DataProvider)dataId -> {
      if (KEY.getName().equals(dataId)) {
        return this;
      }
      else if (PlatformDataKeys.HELP_ID.is(dataId)) {
        return RunDashboardManager.getInstance(myProject).getToolWindowContextHelpId();
      }
      else if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
        return myBuilder.getSelectedElements().toArray();
      }
      Content content = myContentManager.getSelectedContent();
      if (content != null && content.getComponent() != null) {
        DataProvider dataProvider = DataManagerImpl.getDataProviderEx(content.getComponent());
        if (dataProvider != null) {
          return dataProvider.getData(dataId);
        }
      }
      return null;
    });
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent event) {
        if (myLastSelection instanceof RunDashboardRunConfigurationNode && myLastSelection.getChildren().isEmpty()) {
          RunDashboardRunConfigurationNode node = (RunDashboardRunConfigurationNode)myLastSelection;
          for (RunDashboardCustomizer customizer : node.getCustomizers()) {
            PsiElement psiElement = customizer.getPsiElement(node);
            if (psiElement != null) {
              PsiNavigateUtil.navigate(psiElement, true);
              return true;
            }
          }
        }
        return false;
      }
    }.installOn(myTree);

    DefaultActionGroup popupActionGroup = new DefaultActionGroup();
    popupActionGroup.add(ActionManager.getInstance().getAction(RUN_DASHBOARD_CONTENT_TOOLBAR));
    popupActionGroup.addSeparator();
    popupActionGroup.add(ActionManager.getInstance().getAction(RUN_DASHBOARD_TREE_TOOLBAR));
    popupActionGroup.add(ActionManager.getInstance().getAction(RUN_DASHBOARD_POPUP));
    PopupHandler.installPopupHandler(myTree, popupActionGroup, ActionPlaces.RUN_DASHBOARD_POPUP, ActionManager.getInstance());

    new TreeSpeedSearch(myTree, TreeSpeedSearch.NODE_DESCRIPTOR_TOSTRING, true);

    setTreeVisible(dashboardManager.isShowConfigurations());

    // RunDashboardContent may be initialized when some contents have been already added to content manager.
    // Process already added content.
    for (Content content : myContentManager.getContents()) {
      onContentAdded(content);
    }
    Content selectedContent = myContentManager.getSelectedContent();
    if (selectedContent != null) {
      onContentSelectionChanged(selectedContent);
    }
  }

  private void setTreeVisible(boolean visible) {
    myTreePanel.setVisible(visible);
    myToolbar.setBorder(visible ? null : IdeBorderFactory.createBorder(SideBorder.RIGHT));
    if (!visible && myContentManager.getContentCount() > 0) {
      showContentPanel();
    }
  }

  private void updateContentToolbar(Content content) {
    List<AnAction> actions = myContentActions.get(content);

    myContentActionGroup.removeAll();
    myContentActionGroup.addAll(myDashboardContentActions);
    myContentActionGroup.addSeparator();

    if (actions != null) {
      myContentActionGroup.addAll(
        ContainerUtil.filter(actions, action -> !(action instanceof StopAction) && !(action instanceof FakeRerunAction)));
    }
  }

  private void onSelectionChanged() {
    Set<AbstractTreeNode> nodes = myBuilder.getSelectedElements(AbstractTreeNode.class);
    if (nodes.size() != 1) {
      showMessagePanel(ExecutionBundle.message("run.dashboard.empty.selection.message"));
      myLastSelection = null;
      updateContentToolbar(null);
      return;
    }

    AbstractTreeNode<?> node = nodes.iterator().next();
    if (Comparing.equal(node, myLastSelection)) {
      return;
    }

    myLastSelection = node;
    if (node instanceof RunDashboardNode) {
      Content content = ((RunDashboardNode)node).getContent();
      if (content != null && content.getManager() != myContentManager) {
        content = null;
      }
      updateContentToolbar(content);
      if (content != null) {
        if (content != myContentManager.getSelectedContent()) {
          Content toSelect = content;
          // Invoke content selection change later after currently selected content lost a focus.
          SwingUtilities.invokeLater(() -> {
            if (myContentManager.isDisposed() || myContentManager.getIndexOfContent(toSelect) == -1) return;

            // Selected node may changed, we do not need to select content if it doesn't correspond currently selected node.
            if (myLastSelection instanceof RunDashboardNode) {
              if (toSelect != ((RunDashboardNode)myLastSelection).getContent()) return;
            }

            myContentManager.removeContentManagerListener(myContentManagerListener);
            myContentManager.setSelectedContent(toSelect);
            myContentManager.addContentManagerListener(myContentManagerListener);
          });
        }
        showContentPanel();
        return;
      }
      if (node instanceof RunDashboardRunConfigurationNode) {
        showMessagePanel(ExecutionBundle.message("run.dashboard.not.started.configuration.message"));
        return;
      }
    }

    showMessagePanel(ExecutionBundle.message("run.dashboard.empty.selection.message"));
  }

  private void onContentAdded(Content content) {
    RunnerLayoutUiImpl ui = getRunnerLayoutUi(RunContentManagerImpl.getRunContentDescriptorByContent(content));
    if (ui == null) return;

    List<AnAction> leftToolbarActions = ui.getActions();
    myContentActions.put(content, leftToolbarActions);
    updateContentToolbar(content);
  }

  private void onContentSelectionChanged(Content content) {
    myBuilder.queueUpdate().doWhenDone(() -> myBuilder.accept(RunDashboardNode.class, (TreeVisitor<RunDashboardNode>)node -> {
      if (node.getContent() == content) {
        myBuilder.select(node);
      }
      return false;
    }));
    showContentPanel();
  }

  private void showMessagePanel(String text) {
    Content selectedContent = myContentManager.getSelectedContent();
    if (selectedContent != null) {
      // Invoke content selection change later after currently selected content correctly restores its state,
      // since RunnerContentUi performs restoring later after addNotify call chain.
      SwingUtilities.invokeLater(() -> {
        if (myContentManager.isDisposed() || !myContentManager.isSelected(selectedContent)) return;

        // Selected node may changed, we do not need to remove content from selection if it corresponds currently selected node.
        if (myLastSelection instanceof RunDashboardNode) {
          if (selectedContent == ((RunDashboardNode)myLastSelection).getContent()) return;
        }

        myContentManager.removeContentManagerListener(myContentManagerListener);
        myContentManager.removeFromSelection(selectedContent);
        myContentManager.addContentManagerListener(myContentManagerListener);
      });
    }

    myMessagePanel.getEmptyText().setText(text);
    myDetailsPanelLayout.show(myDetailsPanel, MESSAGE_CARD);
  }

  private void showContentPanel() {
    myDetailsPanelLayout.show(myDetailsPanel, CONTENT_CARD);
  }

  private void setupBuilder() {
    RunDashboardTreeStructure structure = new RunDashboardTreeStructure(myProject, myGroupers);
    myBuilder = new AbstractTreeBuilder(myTree, myTreeModel, structure, IndexComparator.INSTANCE) {
      // unique class to simplify search through the logs
      @Override
      protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
        return super.isAutoExpandNode(nodeDescriptor) ||
               !myCollapsedTreeNodeValues.contains(((AbstractTreeNode)nodeDescriptor).getValue());
      }
    };
    myBuilder.initRootNode();
    Disposer.register(this, myBuilder);
  }

  private JComponent createToolbar() {
    JPanel toolBarPanel = new JPanel(new BorderLayout());

    ActionManager actionManager = ActionManager.getInstance();
    AnAction registeredActions = actionManager.getAction(RUN_DASHBOARD_CONTENT_TOOLBAR);
    myDashboardContentActions.add(registeredActions);
    myContentActionGroup.add(myDashboardContentActions);
    ActionToolbar contentActionsToolBar = actionManager.createActionToolbar(PLACE_TOOLBAR, myContentActionGroup, false);
    toolBarPanel.add(contentActionsToolBar.getComponent(), BorderLayout.CENTER);
    contentActionsToolBar.setTargetComponent(this);

    return toolBarPanel;
  }

  private JComponent createTreeToolBar() {
    JPanel toolBarPanel = new JPanel(new BorderLayout());
    toolBarPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT | SideBorder.BOTTOM));

    DefaultActionGroup treeGroup = new DefaultActionGroup();

    TreeExpander treeExpander = new RunDashboardTreeExpander();
    AnAction expandAllAction = CommonActionsManager.getInstance().createExpandAllAction(treeExpander, this);
    treeGroup.add(expandAllAction);

    AnAction collapseAllAction = CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, this);
    treeGroup.add(collapseAllAction);

    treeGroup.addSeparator();
    List<RunDashboardGrouper> groupers =
      ContainerUtil.filter(myGroupers, grouper -> !grouper.getRule().isAlwaysEnabled());
    if (!groupers.isEmpty()) {
      treeGroup.add(new GroupByActionGroup(groupers));
    }
    treeGroup.add(new StatusActionGroup());

    treeGroup.addSeparator();
    AnAction treeActions = ActionManager.getInstance().getAction(RUN_DASHBOARD_TREE_TOOLBAR);
    treeActions.registerCustomShortcutSet(this, null);
    treeGroup.add(treeActions);

    ActionToolbar treeActionsToolBar = ActionManager.getInstance().createActionToolbar(PLACE_TOOLBAR, treeGroup, true);
    toolBarPanel.add(treeActionsToolBar.getComponent(), BorderLayout.CENTER);
    treeActionsToolBar.setTargetComponent(myTree);

    return toolBarPanel;
  }

  @Override
  public void dispose() {
  }

  public void updateContent(boolean withStructure) {
    AppUIUtil.invokeLaterIfProjectAlive(myProject, () -> {
      boolean showConfigurations = RunDashboardManager.getInstance(myProject).isShowConfigurations();
      if (myTreePanel.isVisible() ^ showConfigurations) {
        setTreeVisible(showConfigurations);

        revalidate();
        repaint();

        if (showConfigurations) {
          IdeFocusManager.getInstance(myProject).requestFocus(myTree, true);
        }
      }

      myBuilder.queueUpdate(withStructure).doWhenDone(() -> {
        if (!withStructure) {
          return;
        }
        // Remove nodes not presented in the tree from collapsed node values set.
        // Children retrieving is quick since grouping and run configuration nodes are already constructed.
        Set<Object> nodes = new HashSet<>();
        myBuilder.accept(AbstractTreeNode.class, (TreeVisitor<AbstractTreeNode>)node -> {
          nodes.add(node.getValue());
          return false;
        });
        myCollapsedTreeNodeValues.retainAll(nodes);
      });
    });
  }

  @Override
  @NotNull
  public AbstractTreeBuilder getBuilder() {
    return myBuilder;
  }

  public float getContentProportion() {
    return mySplitter.getProportion();
  }

  private class RunDashboardTreeExpander extends DefaultTreeExpander {
    boolean myFlat;

    RunDashboardTreeExpander() {
      super(myTree);
      myTreeModel.addTreeModelListener(new TreeModelAdapter() {
        @Override
        public void treeStructureChanged(TreeModelEvent e) {
          myFlat = isFlat();
        }
      });
    }

    @Override
    public boolean canExpand() {
      return super.canExpand() && !myFlat;
    }

    @Override
    public boolean canCollapse() {
      return super.canCollapse() && !myFlat;
    }

    private boolean isFlat() {
      Object root = myTreeModel.getRoot();
      for (int i = 0; i < myTreeModel.getChildCount(root); i++) {
        Object child = myTreeModel.getChild(root, i);
        if (myTreeModel.getChildCount(child) > 0) {
          return false;
        }
      }
      return true;
    }
  }

  private class GroupByActionGroup extends DefaultActionGroup implements CheckedActionGroup {
    GroupByActionGroup(List<? extends RunDashboardGrouper> groupers) {
      super(ExecutionBundle.message("run.dashboard.group.by.action.name"), true);
      getTemplatePresentation().setIcon(AllIcons.Actions.GroupBy);

      for (RunDashboardGrouper grouper : groupers) {
        add(new GroupAction(grouper));
      }
    }
  }

  private class GroupAction extends ToggleAction implements DumbAware {
    private final RunDashboardGrouper myGrouper;

    GroupAction(RunDashboardGrouper grouper) {
      super();
      myGrouper = grouper;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      Presentation presentation = e.getPresentation();
      presentation.setEnabled(myTreePanel.isVisible());
      ActionPresentation actionPresentation = myGrouper.getRule().getPresentation();
      presentation.setText(actionPresentation.getText());
      presentation.setDescription(actionPresentation.getDescription());
      if (PLACE_TOOLBAR.equals(e.getPlace())) {
        presentation.setIcon(actionPresentation.getIcon());
      }
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return myGrouper.isEnabled();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      myGrouper.setEnabled(state);
      updateContent(true);
    }
  }

  private static class StatusActionGroup extends RunDashboardFilterActionGroup {
    StatusActionGroup() {
      super(ExecutionBundle.message("run.dashboard.filter.by.status.action.name"), true);
      getTemplatePresentation().setIcon(AllIcons.General.Filter);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
    }
  }
}
