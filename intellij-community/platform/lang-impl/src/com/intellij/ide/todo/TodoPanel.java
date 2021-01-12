// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.todo;

import com.intellij.find.FindModel;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.ide.actions.NextOccurenceToolbarAction;
import com.intellij.ide.actions.PreviousOccurenceToolbarAction;
import com.intellij.ide.todo.nodes.TodoFileNode;
import com.intellij.ide.todo.nodes.TodoItemNode;
import com.intellij.ide.todo.nodes.TodoTreeHelper;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.VisibilityWatcher;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.impl.UsagePreviewPanel;
import com.intellij.util.Alarm;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.tree.TreeModelAdapter;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TodoPanel extends SimpleToolWindowPanel implements OccurenceNavigator, DataProvider, Disposable {
  protected static final Logger LOG = Logger.getInstance(TodoPanel.class);

  protected Project myProject;
  private final TodoPanelSettings mySettings;
  private final boolean myCurrentFileMode;
  private final Content myContent;

  private final Tree myTree;
  private final TreeExpander myTreeExpander;
  private final MyOccurenceNavigator myOccurenceNavigator;
  protected final TodoTreeBuilder myTodoTreeBuilder;
  private MyVisibilityWatcher myVisibilityWatcher;
  private UsagePreviewPanel myUsagePreviewPanel;
  private MyAutoScrollToSourceHandler myAutoScrollToSourceHandler;

  public static final DataKey<TodoPanel> TODO_PANEL_DATA_KEY = DataKey.create("TodoPanel");

  /**
   * @param currentFileMode if {@code true} then view doesn't have "Group By Packages" and "Flatten Packages"
   *                        actions.
   */
  TodoPanel(Project project, TodoPanelSettings settings, boolean currentFileMode, Content content) {
    super(false, true);

    myProject = project;
    mySettings = settings;
    myCurrentFileMode = currentFileMode;
    myContent = content;

    DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode());
    myTree = new Tree(model);
    myTreeExpander = new DefaultTreeExpander(myTree);
    myOccurenceNavigator = new MyOccurenceNavigator();
    initUI();
    myTodoTreeBuilder = setupTreeStructure();
    updateTodoFilter();
    myTodoTreeBuilder.setShowPackages(mySettings.arePackagesShown);
    myTodoTreeBuilder.setShowModules(mySettings.areModulesShown);
    myTodoTreeBuilder.setFlattenPackages(mySettings.areFlattenPackages);

    myVisibilityWatcher = new MyVisibilityWatcher();
    myVisibilityWatcher.install(this);
  }

  private TodoTreeBuilder setupTreeStructure() {
    TodoTreeBuilder todoTreeBuilder = createTreeBuilder(myTree, myProject);
    Disposer.register(this, todoTreeBuilder);
    TodoTreeStructure structure = todoTreeBuilder.getTodoTreeStructure();
    StructureTreeModel<TodoTreeStructure> structureTreeModel = new StructureTreeModel<>(structure, TodoTreeBuilder.NODE_DESCRIPTOR_COMPARATOR, myProject);
    AsyncTreeModel asyncTreeModel = new AsyncTreeModel(structureTreeModel, myProject);
    myTree.setModel(asyncTreeModel);
    asyncTreeModel.addTreeModelListener(new MyExpandListener(todoTreeBuilder));
    todoTreeBuilder.setModel(structureTreeModel);
    Object selectableElement = structure.getFirstSelectableElement();
    if (selectableElement != null) {
      todoTreeBuilder.select(selectableElement);
    }
    return todoTreeBuilder;
  }

  public static class GroupByActionGroup extends DefaultActionGroup {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        JBPopupFactory.getInstance()
          .createActionGroupPopup(null, this, e.getDataContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true)
          .showUnderneathOf(e.getInputEvent().getComponent());
      }
  }

  protected Tree getTree() {
    return myTree;
  }

  private class MyExpandListener extends TreeModelAdapter {

    private final TodoTreeBuilder myBuilder;

    MyExpandListener(TodoTreeBuilder builder) {
      myBuilder = builder;
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
      TreePath parentPath = e.getTreePath();
      if (parentPath == null || parentPath.getPathCount() > 2) return;
      Object[] children = e.getChildren();
      for (Object o : children) {
        NodeDescriptor descriptor = TreeUtil.getUserObject(NodeDescriptor.class, o);
        if (descriptor != null && myBuilder.isAutoExpandNode(descriptor)) {
          ApplicationManager.getApplication().invokeLater(() -> {
            if (myTree.isVisible(parentPath) && myTree.isExpanded(parentPath)) {
              myTree.expandPath(parentPath.pathByAddingChild(o));
            }
          }, myBuilder.myProject.getDisposed());
        }
      }
    }
  }
  
  protected abstract TodoTreeBuilder createTreeBuilder(JTree tree, Project project);

  private void initUI() {
    myTree.setShowsRootHandles(true);
    myTree.setRootVisible(false);
    myTree.setRowHeight(0); // enable variable-height rows
    myTree.setCellRenderer(new TodoCompositeRenderer());
    EditSourceOnDoubleClickHandler.install(myTree);
    EditSourceOnEnterKeyHandler.install(myTree);
    new TreeSpeedSearch(myTree);

    DefaultActionGroup group = new DefaultActionGroup();
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE));
    group.addSeparator();
    group.add(CommonActionsManager.getInstance().createExpandAllAction(myTreeExpander, this));
    group.add(CommonActionsManager.getInstance().createCollapseAllAction(myTreeExpander, this));
    group.addSeparator();
    group.add(ActionManager.getInstance().getAction(IdeActions.GROUP_VERSION_CONTROLS));
    PopupHandler.installPopupHandler(myTree, group, ActionPlaces.TODO_VIEW_POPUP, ActionManager.getInstance());

    myUsagePreviewPanel = new UsagePreviewPanel(myProject, FindInProjectUtil.setupViewPresentation(false, new FindModel()));
    Disposer.register(this, myUsagePreviewPanel);
    myUsagePreviewPanel.setVisible(mySettings.showPreview);

    setContent(createCenterComponent());

    myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(final TreeSelectionEvent e) {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (myUsagePreviewPanel.isVisible()) {
            updatePreviewPanel();
          }
        }, ModalityState.NON_MODAL, myProject.getDisposed());
      }
    });

    myAutoScrollToSourceHandler = new MyAutoScrollToSourceHandler();
    myAutoScrollToSourceHandler.install(myTree);

    // Create tool bars and register custom shortcuts

    DefaultActionGroup toolbarGroup = new DefaultActionGroup();
    toolbarGroup.add(new PreviousOccurenceToolbarAction(myOccurenceNavigator));
    toolbarGroup.add(new NextOccurenceToolbarAction(myOccurenceNavigator));
    toolbarGroup.add(new SetTodoFilterAction(myProject, mySettings, todoFilter -> setTodoFilter(todoFilter)));
    toolbarGroup.add(createAutoScrollToSourceAction());

    if (!myCurrentFileMode) {
      DefaultActionGroup groupBy = createGroupByActionGroup();
      toolbarGroup.add(groupBy);
    }

    toolbarGroup.add(new MyPreviewAction());

    setToolbar(ActionManager.getInstance().createActionToolbar(ActionPlaces.TODO_VIEW_TOOLBAR, toolbarGroup, false).getComponent());
  }

  @NotNull
  protected DefaultActionGroup createGroupByActionGroup() {
    ActionManager actionManager = ActionManager.getInstance();
    return (DefaultActionGroup) actionManager.getAction("TodoViewGroupByGroup");
  }

  protected AnAction createAutoScrollToSourceAction() {
    return myAutoScrollToSourceHandler.createToggleAction();
  }

  protected JComponent createCenterComponent() {
    Splitter splitter = new OnePixelSplitter(false);
    splitter.setSecondComponent(myUsagePreviewPanel);
    splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myTree));
    return splitter;
  }

  private void updatePreviewPanel() {
    if (myProject == null || myProject.isDisposed()) return;
    List<UsageInfo> infos = new ArrayList<>();
    final TreePath path = myTree.getSelectionPath();
    if (path != null) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (userObject instanceof NodeDescriptor) {
        Object element = ((NodeDescriptor)userObject).getElement();
        TodoItemNode pointer = myTodoTreeBuilder.getFirstPointerForElement(element);
        if (pointer != null) {
          final SmartTodoItemPointer value = pointer.getValue();
          final Document document = value.getDocument();
          final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
          final RangeMarker rangeMarker = value.getRangeMarker();
          if (psiFile != null) {
            infos.add(new UsageInfo(psiFile, rangeMarker.getStartOffset(), rangeMarker.getEndOffset()));
            for (RangeMarker additionalMarker: value.getAdditionalRangeMarkers()) {
              if (additionalMarker.isValid()) {
                infos.add(new UsageInfo(psiFile, additionalMarker.getStartOffset(), additionalMarker.getEndOffset()));
              }
            }
          }
        }
      }
    }
    myUsagePreviewPanel.updateLayout(infos.isEmpty() ? null : infos);
  }

  @Override
  public void dispose() {
    if (myVisibilityWatcher != null) {
      myVisibilityWatcher.deinstall(this);
      myVisibilityWatcher = null;
    }
    myProject = null;
  }

  void rebuildCache() {
    myTodoTreeBuilder.rebuildCache();
  }

  void rebuildCache(@NotNull Set<? extends VirtualFile> files) {
    myTodoTreeBuilder.rebuildCache(files);
  }

  /**
   * Immediately updates tree.
   */
  void updateTree() {
    myTodoTreeBuilder.updateTree();
  }

  /**
   * Updates current filter. If previously set filter was removed then empty filter is set.
   *
   * @see TodoTreeBuilder#setTodoFilter
   */
  void updateTodoFilter() {
    TodoFilter filter = TodoConfiguration.getInstance().getTodoFilter(mySettings.todoFilterName);
    setTodoFilter(filter);
  }

  /**
   * Sets specified {@code TodoFilter}. The method also updates window's title.
   *
   * @see TodoTreeBuilder#setTodoFilter
   */
  private void setTodoFilter(TodoFilter filter) {
    // Clear name of current filter if it was removed from configuration.
    String filterName = filter != null ? filter.getName() : null;
    mySettings.todoFilterName = filterName;
    // Update filter
    myTodoTreeBuilder.setTodoFilter(filter);
    // Update content's title
    myContent.setDescription(filterName);
  }

  /**
   * @return list of all selected virtual files.
   */
  @Nullable
  protected PsiFile getSelectedFile() {
    TreePath path = myTree.getSelectionPath();
    if (path == null) {
      return null;
    }
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
    LOG.assertTrue(node != null);
    if(node.getUserObject() == null){
      return null;
    }
    return TodoTreeBuilder.getFileForNode(node);
  }

  protected void setDisplayName(String tabName) {
    myContent.setDisplayName(tabName);
  }

  @Nullable
  private PsiElement getSelectedElement() {
    if (myTree == null) return null;
    TreePath path = myTree.getSelectionPath();
    if (path == null) {
      return null;
    }
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
    Object userObject = node.getUserObject();
    final PsiElement selectedElement = TodoTreeHelper.getInstance(myProject).getSelectedElement(userObject);
    if (selectedElement != null) return selectedElement;
    return getSelectedFile();
  }

  @Override
  public Object getData(@NotNull String dataId) {
    if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return null;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      Object element = ((NodeDescriptor)userObject).getElement();
      if (!(element instanceof TodoFileNode || element instanceof TodoItemNode)) { // allow user to use F4 only on files an TODOs
        return null;
      }
      TodoItemNode pointer = myTodoTreeBuilder.getFirstPointerForElement(element);
      if (pointer != null) {
        return PsiNavigationSupport.getInstance().createNavigatable(myProject,
                                                                    pointer.getValue().getTodoItem().getFile()
                                                                           .getVirtualFile(),
                                                                    pointer.getValue().getRangeMarker()
                                                                           .getStartOffset());
      }
      else {
        return null;
      }
    }
    else if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
      final PsiFile file = getSelectedFile();
      return file != null ? file.getVirtualFile() : null;
    }
    else if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
      return getSelectedElement();
    }
    else if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
      PsiFile file = getSelectedFile();
      if (file != null) {
        return new VirtualFile[]{file.getVirtualFile()};
      }
      else {
        return VirtualFile.EMPTY_ARRAY;
      }
    }
    else if (PlatformDataKeys.HELP_ID.is(dataId)) {
      return "find.todoList";
    }
    else if (TODO_PANEL_DATA_KEY.is(dataId)) {
      return this;
    }
    return super.getData(dataId);
  }

  @Override
  @Nullable
  public OccurenceInfo goPreviousOccurence() {
    return myOccurenceNavigator.goPreviousOccurence();
  }

  @NotNull
  @Override
  public String getNextOccurenceActionName() {
    return myOccurenceNavigator.getNextOccurenceActionName();
  }

  @Override
  @Nullable
  public OccurenceInfo goNextOccurence() {
    return myOccurenceNavigator.goNextOccurence();
  }

  @Override
  public boolean hasNextOccurence() {
    return myOccurenceNavigator.hasNextOccurence();
  }

  @NotNull
  @Override
  public String getPreviousOccurenceActionName() {
    return myOccurenceNavigator.getPreviousOccurenceActionName();
  }

  @Override
  public boolean hasPreviousOccurence() {
    return myOccurenceNavigator.hasPreviousOccurence();
  }

  protected void rebuildWithAlarm(final Alarm alarm) {
    alarm.cancelAllRequests();
    alarm.addRequest(() -> {
      ReadAction.nonBlocking(() -> {
        final Set<VirtualFile> files = new HashSet<>();
        if (myTodoTreeBuilder.isDisposed()) return;
        myTodoTreeBuilder.collectFiles(virtualFile -> {
          files.add(virtualFile);
          return true;
        });
        ApplicationManager.getApplication().invokeLater(() -> {
          if (myTodoTreeBuilder.isDisposed()) return;
          myTodoTreeBuilder.rebuildCache(files);
          updateTree();
        });
      }).executeSynchronously();
    }, 300);
  }

  /**
   * Provides support for "auto scroll to source" functionality
   */
  private final class MyAutoScrollToSourceHandler extends AutoScrollToSourceHandler {
    MyAutoScrollToSourceHandler() {
    }

    @Override
    protected boolean isAutoScrollMode() {
      return mySettings.isAutoScrollToSource;
    }

    @Override
    protected void setAutoScrollMode(boolean state) {
      mySettings.isAutoScrollToSource = state;
    }
  }

  /**
   * Provides support for "Ctrl+Alt+Up/Down" navigation.
   */
  private final class MyOccurenceNavigator implements OccurenceNavigator {
    @Override
    public boolean hasNextOccurence() {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return false;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (userObject == null) {
        return false;
      }
      if (userObject instanceof NodeDescriptor && ((NodeDescriptor)userObject).getElement() instanceof TodoItemNode) {
        return myTree.getRowCount() != myTree.getRowForPath(path) + 1;
      }
      else {
        return node.getChildCount() > 0;
      }
    }

    @Override
    public boolean hasPreviousOccurence() {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return false;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      return userObject instanceof NodeDescriptor && !isFirst(node);
    }

    private boolean isFirst(final TreeNode node) {
      final TreeNode parent = node.getParent();
      return parent == null || parent.getIndex(node) == 0 && isFirst(parent);
    }

    @Override
    @Nullable
    public OccurenceInfo goNextOccurence() {
      return goToPointer(getNextPointer());
    }

    @Override
    @Nullable
    public OccurenceInfo goPreviousOccurence() {
      return goToPointer(getPreviousPointer());
    }

    @NotNull
    @Override
    public String getNextOccurenceActionName() {
      return IdeBundle.message("action.next.todo");
    }

    @NotNull
    @Override
    public String getPreviousOccurenceActionName() {
      return IdeBundle.message("action.previous.todo");
    }

    @Nullable
    private OccurenceInfo goToPointer(TodoItemNode pointer) {
      if (pointer == null) return null;
      myTodoTreeBuilder.select(pointer);
      return new OccurenceInfo(
        PsiNavigationSupport.getInstance()
                            .createNavigatable(myProject, pointer.getValue().getTodoItem().getFile().getVirtualFile(),
                                               pointer.getValue().getRangeMarker().getStartOffset()),
        -1,
        -1
      );
    }

    @Nullable
    private TodoItemNode getNextPointer() {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return null;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      Object element = ((NodeDescriptor)userObject).getElement();
      TodoItemNode pointer;
      if (element instanceof TodoItemNode) {
        pointer = myTodoTreeBuilder.getNextPointer((TodoItemNode)element);
      }
      else {
        pointer = myTodoTreeBuilder.getFirstPointerForElement(element);
      }
      return pointer;
    }

    @Nullable
    private TodoItemNode getPreviousPointer() {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return null;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      Object element = ((NodeDescriptor)userObject).getElement();
      TodoItemNode pointer;
      if (element instanceof TodoItemNode) {
        pointer = myTodoTreeBuilder.getPreviousPointer((TodoItemNode)element);
      }
      else {
        Object sibling = myTodoTreeBuilder.getPreviousSibling(element);
        if (sibling == null) {
          return null;
        }
        pointer = myTodoTreeBuilder.getLastPointerForElement(sibling);
      }
      return pointer;
    }
  }

  public static final class MyShowPackagesAction extends ToggleAction {
    public MyShowPackagesAction() {
      super(IdeBundle.messagePointer("action.group.by.packages"), PlatformIcons.GROUP_BY_PACKAGES);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(e.getData(TODO_PANEL_DATA_KEY) != null);
      super.update(e);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      return todoPanel != null && todoPanel.mySettings.arePackagesShown;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      if (todoPanel != null) {
        todoPanel.mySettings.arePackagesShown = state;
        todoPanel.myTodoTreeBuilder.setShowPackages(state);
      }
    }
  }

  public static final class MyShowModulesAction extends ToggleAction {
    public MyShowModulesAction() {
      super(IdeBundle.messagePointer("action.group.by.modules"), AllIcons.Actions.GroupByModule);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(e.getData(TODO_PANEL_DATA_KEY) != null);
      super.update(e);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      return todoPanel != null && todoPanel.mySettings.areModulesShown;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);

      if (todoPanel != null) {
        todoPanel.mySettings.areModulesShown = state;
        todoPanel.myTodoTreeBuilder.setShowModules(state);
      }
    }
  }

  public static final class MyFlattenPackagesAction extends ToggleAction {
    public MyFlattenPackagesAction() {
      super(IdeBundle.messagePointer("action.flatten.view"), PlatformIcons.FLATTEN_PACKAGES_ICON);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setText("   " + getTemplateText());
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      e.getPresentation().setEnabled(todoPanel != null && todoPanel.mySettings.arePackagesShown);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      return todoPanel != null && todoPanel.mySettings.areFlattenPackages;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      if (todoPanel != null) {
        todoPanel.mySettings.areFlattenPackages = state;
        todoPanel.myTodoTreeBuilder.setFlattenPackages(state);
      }
    }
  }

  private final class MyVisibilityWatcher extends VisibilityWatcher {
    @Override
    public void visibilityChanged() {
      if (myProject.isOpen()) {
        PsiDocumentManager.getInstance(myProject).performWhenAllCommitted(
          () -> myTodoTreeBuilder.setUpdatable(isShowing()));
      }
    }
  }

  private final class MyPreviewAction extends ToggleAction {

    MyPreviewAction() {
      super(IdeBundle.messagePointer("todo.panel.preview.source.action.text"), Presentation.NULL_STRING, AllIcons.Actions.PreviewDetails);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return mySettings.showPreview;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      mySettings.showPreview = state;
      myUsagePreviewPanel.setVisible(state);
      if (state) {
        updatePreviewPanel();
      }
    }
  }
}
