// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.build.events.*;
import com.intellij.build.events.impl.FailureResultImpl;
import com.intellij.build.events.impl.SkippedResultImpl;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.ide.actions.EditSourceAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.pom.NonNavigatable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.*;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.tree.ui.DefaultTreeUI;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.intellij.build.BuildView.CONSOLE_VIEW_NAME;
import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;
import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static com.intellij.util.ui.UIUtil.getTreeSelectionForeground;

/**
 * @author Vladislav.Soroka
 */
public class BuildTreeConsoleView implements ConsoleView, DataProvider, BuildConsoleView, Filterable<ExecutionNode> {
  private static final Logger LOG = Logger.getInstance(BuildTreeConsoleView.class);

  @NonNls private static final String TREE = "tree";
  @NonNls private static final String SPLITTER_PROPERTY = "BuildView.Splitter.Proportion";
  private final JPanel myPanel = new JPanel();
  private final Map<Object, ExecutionNode> nodesMap = ContainerUtil.newConcurrentMap();

  private final Project myProject;
  private final ConsoleViewHandler myConsoleViewHandler;
  private final String myWorkingDir;
  private final AtomicBoolean myDisposed = new AtomicBoolean();
  private final AtomicBoolean myShownFirstError = new AtomicBoolean();
  private final StructureTreeModel<SimpleTreeStructure> myTreeModel;
  private final Tree myTree;
  private final ExecutionNode myRootNode;
  private final ExecutionNode myBuildProgressRootNode;
  @Nullable
  private volatile Predicate<ExecutionNode> myExecutionTreeFilter;

  public BuildTreeConsoleView(Project project,
                              BuildDescriptor buildDescriptor,
                              @Nullable ExecutionConsole executionConsole,
                              @NotNull BuildViewSettingsProvider buildViewSettingsProvider) {
    myProject = project;
    myWorkingDir = FileUtil.toSystemIndependentName(buildDescriptor.getWorkingDir());

    myRootNode = new ExecutionNode(myProject, null);
    myRootNode.setAutoExpandNode(true);
    myBuildProgressRootNode = new ExecutionNode(myProject, myRootNode);
    myBuildProgressRootNode.setAutoExpandNode(true);
    myRootNode.add(myBuildProgressRootNode);

    SimpleTreeStructure treeStructure = new SimpleTreeStructure.Impl(myRootNode);
    myTreeModel = new StructureTreeModel<>(treeStructure);
    myTree = initTree(new AsyncTreeModel(myTreeModel, this));

    JPanel myContentPanel = new JPanel();
    myContentPanel.setLayout(new CardLayout());
    myContentPanel.add(ScrollPaneFactory.createScrollPane(myTree, SideBorder.NONE), TREE);

    myPanel.setLayout(new BorderLayout());
    OnePixelSplitter myThreeComponentsSplitter = new OnePixelSplitter(SPLITTER_PROPERTY, 0.33f);
    myThreeComponentsSplitter.setFirstComponent(myContentPanel);
    myConsoleViewHandler =
      new ConsoleViewHandler(myProject, myTree, myBuildProgressRootNode, this, executionConsole, buildViewSettingsProvider);
    myThreeComponentsSplitter.setSecondComponent(myConsoleViewHandler.getComponent());
    myPanel.add(myThreeComponentsSplitter, BorderLayout.CENTER);
  }

  private void installContextMenu(@NotNull StartBuildEvent startBuildEvent) {
    UIUtil.invokeLaterIfNeeded(() -> {
      final DefaultActionGroup group = new DefaultActionGroup();
      final DefaultActionGroup rerunActionGroup = new DefaultActionGroup();
      AnAction[] restartActions = startBuildEvent.getRestartActions();
      for (AnAction anAction : restartActions) {
        rerunActionGroup.add(anAction);
      }
      if (restartActions.length > 0) {
        group.addAll(rerunActionGroup);
        group.addSeparator();
      }
      EditSourceAction edit = new EditSourceAction();
      ActionUtil.copyFrom(edit, "EditSource");
      group.add(edit);
      group.addSeparator();
      group.add(new ShowExecutionErrorsOnlyAction(this));

      PopupHandler.installPopupHandler(myTree, group, "BuildView");
    });
  }

  @Override
  public void clear() {
    getRootElement().removeChildren();
    nodesMap.clear();
    myConsoleViewHandler.clear();
    myTreeModel.invalidate();
  }

  @Override
  public boolean isFilteringEnabled() {
    return true;
  }

  @Override
  @Nullable
  public Predicate<ExecutionNode> getFilter() {
    return myExecutionTreeFilter;
  }

  @Override
  public void setFilter(@Nullable Predicate<ExecutionNode> executionTreeFilter) {
    myExecutionTreeFilter = executionTreeFilter;
    ExecutionNode buildProgressRootNode = getBuildProgressRootNode();
    ExecutionNode rootElement = getRootElement();
    Predicate<ExecutionNode> predicate = executionTreeFilter == null ? null :
                                         node -> node == buildProgressRootNode || executionTreeFilter.test(node);
    rootElement.setFilter(predicate);
    scheduleUpdate(rootElement);
  }

  private ExecutionNode getRootElement() {
    return myRootNode;
  }

  private ExecutionNode getBuildProgressRootNode() {
    return myBuildProgressRootNode;
  }

  @Override
  public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
  }

  @Nullable
  private ExecutionNode getOrMaybeCreateParentNode(@NotNull BuildEvent event) {
    ExecutionNode parentNode = event.getParentId() == null ? null : nodesMap.get(event.getParentId());
    if (event instanceof MessageEvent) {
      parentNode = createMessageParentNodes((MessageEvent)event, parentNode);
    }
    return parentNode;
  }

  private void onEventInternal(@NotNull BuildEvent event) {
    final ExecutionNode parentNode = getOrMaybeCreateParentNode(event);
    final Object eventId = event.getId();
    ExecutionNode currentNode = nodesMap.get(eventId);
    ExecutionNode buildProgressRootNode = getBuildProgressRootNode();
    if (event instanceof StartEvent || event instanceof MessageEvent) {
      if (currentNode == null) {
        if (event instanceof StartBuildEvent) {
          currentNode = buildProgressRootNode;
          installContextMenu((StartBuildEvent)event);
          String buildTitle = ((StartBuildEvent)event).getBuildTitle();
          currentNode.setTitle(buildTitle);
          currentNode.setAutoExpandNode(true);
        }
        else {
          currentNode = new ExecutionNode(myProject, parentNode);

          if (event instanceof MessageEvent) {
            MessageEvent messageEvent = (MessageEvent)event;
            currentNode.setStartTime(messageEvent.getEventTime());
            currentNode.setEndTime(messageEvent.getEventTime());
            Navigatable messageEventNavigatable = messageEvent.getNavigatable(myProject);
            currentNode.setNavigatable(messageEventNavigatable);
            MessageEventResult messageEventResult = messageEvent.getResult();
            currentNode.setResult(messageEventResult);

            if (messageEventResult instanceof FailureResult) {
              for (Failure failure : ((FailureResult)messageEventResult).getFailures()) {
                showErrorIfFirst(currentNode, failure.getNavigatable());
              }
            }
            if (messageEvent.getKind() == MessageEvent.Kind.ERROR) {
              showErrorIfFirst(currentNode, messageEventNavigatable);
            }

            if (parentNode != buildProgressRootNode) {
              myConsoleViewHandler.addOutput(parentNode, event);
              myConsoleViewHandler.addOutput(parentNode, "\n", true);
            }
            myConsoleViewHandler.addOutput(currentNode, event);
          }
          currentNode.setAutoExpandNode(currentNode == buildProgressRootNode || parentNode == buildProgressRootNode);
        }
        nodesMap.put(eventId, currentNode);
      }
      else {
        LOG.warn("start event id collision found:" + eventId + ", was also in node: " + currentNode.getTitle());
        return;
      }

      if (parentNode != null) {
        parentNode.add(currentNode);
      }
    }
    else {
      currentNode = nodesMap.get(eventId);
      if (currentNode == null) {
        if (event instanceof ProgressBuildEvent) {
          currentNode = new ExecutionNode(myProject, parentNode);
          nodesMap.put(eventId, currentNode);
          if (parentNode != null) {
            parentNode.add(currentNode);
          }
        }
        else if (event instanceof OutputBuildEvent && parentNode != null) {
          myConsoleViewHandler.addOutput(parentNode, event);
        }
      }
    }

    if (currentNode == null) {
      return;
    }

    currentNode.setName(event.getMessage());
    currentNode.setHint(event.getHint());
    if (currentNode.getStartTime() == 0) {
      currentNode.setStartTime(event.getEventTime());
    }

    if (event instanceof FinishEvent) {
      currentNode.setEndTime(event.getEventTime());
      currentNode.setResult(((FinishEvent)event).getResult());
      SkippedResult skippedResult = new SkippedResultImpl();
      finishChildren(currentNode, skippedResult);
      if (((FinishEvent)event).getResult() instanceof FailureResult) {
        for (Failure failure : ((FailureResult)((FinishEvent)event).getResult()).getFailures()) {
          addChildFailureNode(currentNode, failure, event.getMessage());
        }
      }
    }

    if (event instanceof FinishBuildEvent) {
      String aHint = event.getHint();
      String time = DateFormatUtil.formatDateTime(event.getEventTime());
      aHint = aHint == null ? "at " + time : aHint + " at " + time;
      currentNode.setHint(aHint);
      if (myConsoleViewHandler.myExecutionNode == null) {
        ApplicationManager.getApplication().invokeLater(() -> myConsoleViewHandler.setNode(buildProgressRootNode));
      }
    }
    scheduleUpdate(currentNode);
  }

  private void showErrorIfFirst(@NotNull ExecutionNode node, @Nullable Navigatable navigatable) {
    if (myShownFirstError.compareAndSet(false, true)) {
      if (navigatable != null && navigatable != NonNavigatable.INSTANCE) {
        ApplicationManager.getApplication()
          .invokeLater(() -> navigatable.navigate(false), ModalityState.defaultModalityState(), myProject.getDisposed());
      }
      SimpleNode parentOrNode = node.getParent() == null ? node : node.getParent();
      myTreeModel.invalidate(parentOrNode, true).onProcessed(p -> TreeUtil.promiseSelect(myTree, visitor(node)));
    }
  }

  @NotNull
  private static TreeVisitor visitor(@NotNull ExecutionNode executionNode) {
    return path -> {
      Object object = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
      if (executionNode == object) return TreeVisitor.Action.INTERRUPT;
      return TreeVisitor.Action.CONTINUE;
    };
  }

  private void addChildFailureNode(@NotNull ExecutionNode parentNode, @NotNull Failure failure, @NotNull String defaultFailureMessage) {
    ExecutionNode failureNode = new ExecutionNode(myProject, parentNode);
    parentNode.add(failureNode);
    failureNode.setNavigatable(failure.getNavigatable());
    failureNode.setResult(new FailureResultImpl(Collections.singletonList(failure)));
    String text = ObjectUtils.chooseNotNull(failure.getDescription(), failure.getMessage());
    if (text == null && failure.getError() != null) {
      text = failure.getError().getMessage();
    }
    if (text == null) {
      text = defaultFailureMessage;
    }
    text = StringUtil.trimEnd(ObjectUtils.notNull(StringUtil.substringBefore(text, "\n"), text), '.');
    failureNode.setName(text);
    myConsoleViewHandler.addOutput(failureNode, failure);
    showErrorIfFirst(failureNode, failure.getNavigatable());
  }

  private static void finishChildren(@NotNull ExecutionNode node, @NotNull EventResult result) {
    for (SimpleNode child : node.getChildren()) {
      if (child instanceof ExecutionNode) {
        ExecutionNode executionChild = (ExecutionNode)child;
        if (!executionChild.isRunning()) {
          continue;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
          // Need to check again since node could have finished on a later event.
          if (executionChild.isRunning()) {
            finishChildren(executionChild, result);
            executionChild.setResult(result);
          }
        });
      }
    }
  }

  protected void expand(Tree tree) {
    TreeUtil.expand(tree,
                    path -> {
                      ExecutionNode node = TreeUtil.getLastUserObject(ExecutionNode.class, path);
                      if (node != null && node.isAutoExpandNode() && node.getChildCount() > 0) {
                        return TreeVisitor.Action.CONTINUE;
                      }
                      else {
                        return TreeVisitor.Action.SKIP_CHILDREN;
                      }
                    },
                    path -> {
                    });
  }

  @Override
  public void scrollTo(int offset) {
  }

  @Override
  public void attachToProcess(ProcessHandler processHandler) {
  }

  @Override
  public boolean isOutputPaused() {
    return false;
  }

  @Override
  public void setOutputPaused(boolean value) {
  }

  @Override
  public boolean hasDeferredOutput() {
    return false;
  }

  @Override
  public void performWhenNoDeferredOutput(@NotNull Runnable runnable) {
  }

  @Override
  public void setHelpId(@NotNull String helpId) {
  }

  @Override
  public void addMessageFilter(@NotNull Filter filter) {
  }

  @Override
  public void printHyperlink(@NotNull String hyperlinkText, @Nullable HyperlinkInfo info) {
  }

  @Override
  public int getContentSize() {
    return 0;
  }

  @Override
  public boolean canPause() {
    return false;
  }

  @NotNull
  @Override
  public AnAction[] createConsoleActions() {
    return AnAction.EMPTY_ARRAY;
  }

  @Override
  public void allowHeavyFilters() {
  }

  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return myTree;
  }

  @Override
  public void dispose() {
    myDisposed.set(true);
  }

  public boolean isDisposed() {
    return myDisposed.get();
  }

  @Override
  public void onEvent(@NotNull BuildEvent event) {
    myTreeModel.getInvoker().runOrInvokeLater(() -> onEventInternal(event));
  }

  void scheduleUpdate(ExecutionNode executionNode) {
    SimpleNode node = executionNode.getParent() == null ? executionNode : executionNode.getParent();
    myTreeModel.invalidate(node, true).onProcessed(p -> expand(myTree));
  }

  private ExecutionNode createMessageParentNodes(MessageEvent messageEvent, ExecutionNode parentNode) {
    Object messageEventParentId = messageEvent.getParentId();
    if (messageEventParentId == null) return null;

    String group = messageEvent.getGroup();
    String groupNodeId = group.hashCode() + messageEventParentId.toString();
    final MessageEvent.Kind eventKind = messageEvent.getKind();
    if (messageEvent instanceof FileMessageEvent) {
      FilePosition filePosition = ((FileMessageEvent)messageEvent).getFilePosition();
      String filePath = FileUtil.toSystemIndependentName(filePosition.getFile().getPath());
      String parentsPath = "";

      String relativePath = FileUtil.getRelativePath(myWorkingDir, filePath, '/');
      if (relativePath != null) {
        parentsPath = myWorkingDir;
      }

      String fileNodeId = groupNodeId + filePath;
      relativePath = StringUtil.isEmpty(parentsPath) ? filePath : FileUtil.getRelativePath(parentsPath, filePath, '/');
      parentNode = getOrCreateMessagesNode(messageEvent, fileNodeId, parentNode, relativePath,
                                           () -> {
                                             VirtualFile file = VfsUtil.findFileByIoFile(filePosition.getFile(), false);
                                             if (file != null) {
                                               return file.getFileType().getIcon();
                                             }
                                             return null;
                                           }, messageEvent.getNavigatable(myProject), nodesMap, myProject);
    }

    if (eventKind == MessageEvent.Kind.ERROR || eventKind == MessageEvent.Kind.WARNING) {
      SimpleNode p = parentNode;
      do {
        ((ExecutionNode)p).reportChildMessageKind(eventKind);
      }
      while ((p = p.getParent()) instanceof ExecutionNode);
      scheduleUpdate(getRootElement());
    }
    return parentNode;
  }

  public void hideRootNode() {
    UIUtil.invokeLaterIfNeeded(() -> {
      if (myTree != null) {
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);
      }
    });
  }

  @Nullable
  @Override
  public Object getData(@NotNull String dataId) {
    if (PlatformDataKeys.HELP_ID.is(dataId)) return "reference.build.tool.window";
    if (CommonDataKeys.PROJECT.is(dataId)) return myProject;
    if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) return extractNavigatables();
    return null;
  }

  private Object extractNavigatables() {
    final List<Navigatable> navigatables = new ArrayList<>();
    for (ExecutionNode each : getSelectedNodes()) {
      List<Navigatable> navigatable = each.getNavigatables();
      navigatables.addAll(navigatable);
    }
    return navigatables.isEmpty() ? null : navigatables.toArray(new Navigatable[0]);
  }

  private ExecutionNode[] getSelectedNodes() {
    final ExecutionNode[] result = new ExecutionNode[0];
    if (myTree != null) {
      final List<ExecutionNode> nodes =
        TreeUtil.collectSelectedObjects(myTree, path -> TreeUtil.getLastUserObject(ExecutionNode.class, path));
      return nodes.toArray(result);
    }
    return result;
  }

  @TestOnly
  JTree getTree() {
    return myTree;
  }

  private static Tree initTree(@NotNull AsyncTreeModel model) {
    Tree tree = new MyTree(model);
    UIUtil.putClientProperty(tree, ANIMATION_IN_RENDERER_ALLOWED, true);
    tree.setRootVisible(false);
    EditSourceOnDoubleClickHandler.install(tree);
    EditSourceOnEnterKeyHandler.install(tree, null);
    new TreeSpeedSearch(tree).setComparator(new SpeedSearchComparator(false));
    TreeUtil.installActions(tree);
    tree.setCellRenderer(new MyNodeRenderer());
    return tree;
  }

  @NotNull
  private static ExecutionNode getOrCreateMessagesNode(MessageEvent messageEvent,
                                                       String nodeId,
                                                       ExecutionNode parentNode,
                                                       String nodeName,
                                                       @Nullable Supplier<? extends Icon> iconProvider,
                                                       @Nullable Navigatable navigatable,
                                                       Map<Object, ExecutionNode> nodesMap,
                                                       Project project) {
    ExecutionNode node = nodesMap.get(nodeId);
    if (node == null) {
      node = new ExecutionNode(project, parentNode);
      node.setName(nodeName);
      node.setAutoExpandNode(true);
      node.setStartTime(messageEvent.getEventTime());
      node.setEndTime(messageEvent.getEventTime());
      if (iconProvider != null) {
        node.setIconProvider(iconProvider);
      }
      if (navigatable != null) {
        node.setNavigatable(navigatable);
      }
      parentNode.add(node);
      nodesMap.put(nodeId, node);
    }
    return node;
  }

  private static class ConsoleViewHandler {
    private static final String EMPTY_CONSOLE_NAME = "empty";
    private final Project myProject;
    private final JPanel myPanel;
    private final CompositeView<ExecutionConsole> myView;
    private final AtomicReference<String> myNodeConsoleViewName = new AtomicReference<>();
    private final Map<String, List<Consumer<BuildTextConsoleView>>> deferredNodeOutput = ContainerUtil.newConcurrentMap();
    @NotNull
    private final BuildViewSettingsProvider myViewSettingsProvider;
    @Nullable
    private ExecutionNode myExecutionNode;

    ConsoleViewHandler(@NotNull Project project,
                       @NotNull Tree tree,
                       @NotNull ExecutionNode buildProgressRootNode,
                       @NotNull Disposable parentDisposable,
                       @Nullable ExecutionConsole executionConsole,
                       @NotNull BuildViewSettingsProvider buildViewSettingsProvider) {
      myProject = project;
      myPanel = new JPanel(new BorderLayout());
      myViewSettingsProvider = buildViewSettingsProvider;
      myView = new CompositeView<>(null);
      if (executionConsole != null && buildViewSettingsProvider.isSideBySideView()) {
        String nodeConsoleViewName = getNodeConsoleViewName(buildProgressRootNode);
        myView.addView(executionConsole, nodeConsoleViewName, true);
        myNodeConsoleViewName.set(nodeConsoleViewName);
      }
      ConsoleView emptyConsole = new ConsoleViewImpl(project, GlobalSearchScope.EMPTY_SCOPE, true, false);
      myView.addView(emptyConsole, EMPTY_CONSOLE_NAME, false);
      if (!buildViewSettingsProvider.isSideBySideView()) {
        myPanel.setVisible(false);
      }
      JComponent consoleComponent = emptyConsole.getComponent();
      AnAction[] consoleActions = emptyConsole.createConsoleActions();
      consoleComponent.setFocusable(true);
      final Color editorBackground = EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground();
      consoleComponent.setBorder(new CompoundBorder(IdeBorderFactory.createBorder(SideBorder.RIGHT),
                                                    new SideBorder(editorBackground, SideBorder.LEFT)));
      myPanel.add(myView.getComponent(), BorderLayout.CENTER);
      final ActionToolbar toolbar = ActionManager.getInstance()
        .createActionToolbar("BuildResults", new DefaultActionGroup(consoleActions), false);
      myPanel.add(toolbar.getComponent(), BorderLayout.EAST);
      tree.addTreeSelectionListener(e -> {
        TreePath path = e.getPath();
        if (path == null || !e.isAddedPath()) {
          return;
        }
        TreePath selectionPath = tree.getSelectionPath();
        setNode(selectionPath != null ? (DefaultMutableTreeNode)selectionPath.getLastPathComponent() : null);
      });

      Disposer.register(parentDisposable, myView);
      Disposer.register(parentDisposable, emptyConsole);
    }

    private boolean setNode(@NotNull ExecutionNode node) {
      String nodeConsoleViewName = getNodeConsoleViewName(node);
      List<Consumer<BuildTextConsoleView>> deferredOutput = deferredNodeOutput.get(nodeConsoleViewName);
      myNodeConsoleViewName.set(nodeConsoleViewName);
      ExecutionConsole view = myView.getView(nodeConsoleViewName);
      if (view != null) {
        if (view instanceof BuildTextConsoleView && deferredOutput != null && !deferredOutput.isEmpty()) {
          deferredNodeOutput.remove(nodeConsoleViewName);
          deferredOutput.forEach(consumer -> consumer.accept((BuildTextConsoleView)view));
        }
        myView.enableView(nodeConsoleViewName, false);
        myPanel.setVisible(true);
        return true;
      }

      if (deferredOutput != null && !deferredOutput.isEmpty()) {
        BuildTextConsoleView textConsoleView = new BuildTextConsoleView(myProject, true);
        deferredNodeOutput.remove(nodeConsoleViewName);
        deferredOutput.forEach(consumer -> consumer.accept(textConsoleView));
        myView.addView(textConsoleView, nodeConsoleViewName, false);
        myView.enableView(nodeConsoleViewName, false);
      }
      else if (myViewSettingsProvider.isSideBySideView()) {
        myView.enableView(EMPTY_CONSOLE_NAME, false);
        return true;
      }

      if (!myViewSettingsProvider.isSideBySideView()) {
        EventResult eventResult = node.getResult();
        BuildTextConsoleView taskOutputView = new BuildTextConsoleView(myProject, true);
        boolean hasChanged = taskOutputView.appendEventResult(eventResult);
        if (!hasChanged) return false;

        taskOutputView.scrollTo(0);
        myView.addView(taskOutputView, nodeConsoleViewName, false);
        myView.enableView(nodeConsoleViewName, false);
        myPanel.setVisible(true);
      }
      return true;
    }

    private void addOutput(ExecutionNode node, @NotNull String text, boolean stdOut) {
      addOutput(node, view -> view.append(text, stdOut));
    }

    private void addOutput(ExecutionNode node, BuildEvent event) {
      addOutput(node, view -> view.onEvent(event));
    }

    private void addOutput(ExecutionNode node, Failure failure) {
      addOutput(node, view -> view.append(failure));
    }

    private void addOutput(ExecutionNode node, Consumer<BuildTextConsoleView> consumer) {
      if (!myViewSettingsProvider.isSideBySideView()) return;
      String nodeConsoleViewName = getNodeConsoleViewName(node);
      ExecutionConsole viewView = myView.getView(nodeConsoleViewName);
      if (viewView instanceof BuildTextConsoleView) {
        consumer.accept((BuildTextConsoleView)viewView);
      }
      if (viewView == null) {
        deferredNodeOutput.computeIfAbsent(nodeConsoleViewName, s -> new ArrayList<>()).add(consumer);
      }
    }

    @NotNull
    private static String getNodeConsoleViewName(@NotNull ExecutionNode node) {
      return String.valueOf(System.identityHashCode(node));
    }

    private void setNode(@Nullable DefaultMutableTreeNode node) {
      if (node == null || node.getUserObject() == myExecutionNode) return;
      if (node.getUserObject() instanceof ExecutionNode) {
        myExecutionNode = (ExecutionNode)node.getUserObject();
        if (setNode((ExecutionNode)node.getUserObject())) {
          return;
        }
      }

      myExecutionNode = null;
      if (myView.getView(CONSOLE_VIEW_NAME) != null/* && myViewSettingsProvider.isSideBySideView()*/) {
        myView.enableView(CONSOLE_VIEW_NAME, false);
        myPanel.setVisible(true);
      }
      else {
        myPanel.setVisible(false);
      }
    }

    public JComponent getComponent() {
      return myPanel;
    }

    public void clear() {
      myPanel.setVisible(false);
    }
  }

  private static class MyTree extends Tree {
    private MyTree(TreeModel treemodel) {
      super(treemodel);
    }

    @Override
    public void setUI(final TreeUI ui) {
      super.setUI(ui instanceof DefaultTreeUI ? ui : new DefaultTreeUI());
      setLargeModel(true);
    }
  }

  private static class MyNodeRenderer extends NodeRenderer {
    private String myDurationText;
    private Color myDurationColor;
    private int myDurationWidth;
    private int myDurationOffset;

    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
      myDurationText = null;
      myDurationColor = null;
      myDurationWidth = 0;
      myDurationOffset = 0;
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
      final Object userObj = node.getUserObject();
      if (userObj instanceof ExecutionNode) {
        myDurationText = ((ExecutionNode)userObj).getDuration();
        if (myDurationText != null) {
          FontMetrics metrics = getFontMetrics(RelativeFont.SMALL.derive(getFont()));
          myDurationWidth = metrics.stringWidth(myDurationText);
          myDurationOffset = metrics.getHeight() / 2; // an empty area before and after the text
          myDurationColor = selected ? getTreeSelectionForeground(hasFocus) : GRAYED_ATTRIBUTES.getFgColor();
        }
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      UISettings.setupAntialiasing(g);
      Shape clip = null;
      int width = getWidth();
      int height = getHeight();
      if (isOpaque()) {
        // paint background for expanded row
        g.setColor(getBackground());
        g.fillRect(0, 0, width, height);
      }
      if (myDurationWidth > 0) {
        width -= myDurationWidth + myDurationOffset;
        if (width > 0 && height > 0) {
          g.setColor(myDurationColor);
          g.setFont(RelativeFont.SMALL.derive(getFont()));
          g.drawString(myDurationText, width + myDurationOffset / 2, getTextBaseLine(g.getFontMetrics(), height));
          clip = g.getClip();
          g.clipRect(0, 0, width, height);
        }
      }

      super.paintComponent(g);
      // restore clip area if needed
      if (clip != null) g.setClip(clip);
    }
  }
}
