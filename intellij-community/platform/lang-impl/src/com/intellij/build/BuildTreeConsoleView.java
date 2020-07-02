// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import com.intellij.icons.AllIcons;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.OccurenceNavigator;
import com.intellij.ide.OccurenceNavigatorSupport;
import com.intellij.ide.actions.EditSourceAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.pom.NonNavigatable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.*;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreePathUtil;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SystemProperties;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.SmartHashSet;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.*;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.intellij.build.BuildConsoleUtils.getMessageTitle;
import static com.intellij.build.BuildView.CONSOLE_VIEW_NAME;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;
import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;
import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static com.intellij.ui.render.RenderingHelper.SHRINK_LONG_RENDERER;
import static com.intellij.ui.tree.ui.DefaultTreeUI.AUTO_EXPAND_ALLOWED;
import static com.intellij.util.ObjectUtils.chooseNotNull;
import static com.intellij.util.containers.ContainerUtil.addIfNotNull;
import static com.intellij.util.ui.UIUtil.*;

/**
 * @author Vladislav.Soroka
 */
public class BuildTreeConsoleView implements ConsoleView, DataProvider, BuildConsoleView, Filterable<ExecutionNode>, OccurenceNavigator {
  private static final Logger LOG = Logger.getInstance(BuildTreeConsoleView.class);

  @NonNls private static final String TREE = "tree";
  @NonNls private static final String SPLITTER_PROPERTY = "BuildView.Splitter.Proportion";
  private final JPanel myPanel = new JPanel();
  private final Map<Object, ExecutionNode> nodesMap = new ConcurrentHashMap<>();

  private final @NotNull Project myProject;
  private final @NotNull DefaultBuildDescriptor myBuildDescriptor;
  private final @NotNull String myWorkingDir;
  private final ConsoleViewHandler myConsoleViewHandler;
  private final AtomicBoolean myFinishedBuildEventReceived = new AtomicBoolean();
  private final AtomicBoolean myDisposed = new AtomicBoolean();
  private final AtomicBoolean myShownFirstError = new AtomicBoolean();
  private final AtomicBoolean myExpandedFirstMessage = new AtomicBoolean();
  private final boolean myFocusFirstError;
  private final StructureTreeModel<AbstractTreeStructure> myTreeModel;
  private final Tree myTree;
  private final ExecutionNode myRootNode;
  private final ExecutionNode myBuildProgressRootNode;
  private final Set<Predicate<ExecutionNode>> myNodeFilters;
  private final ProblemOccurrenceNavigatorSupport myOccurrenceNavigatorSupport;
  private final Set<BuildEvent> myDeferredEvents = ContainerUtil.newConcurrentSet();

  public BuildTreeConsoleView(@NotNull Project project,
                              @NotNull BuildDescriptor buildDescriptor,
                              @Nullable ExecutionConsole executionConsole,
                              @NotNull BuildViewSettingsProvider buildViewSettingsProvider) {
    myProject = project;
    myBuildDescriptor = buildDescriptor instanceof DefaultBuildDescriptor
                        ? (DefaultBuildDescriptor)buildDescriptor
                        : new DefaultBuildDescriptor(buildDescriptor);
    myNodeFilters = ContainerUtil.newConcurrentSet();
    myWorkingDir = FileUtil.toSystemIndependentName(buildDescriptor.getWorkingDir());
    myFocusFirstError = !(buildDescriptor instanceof DefaultBuildDescriptor) ||
                        ((DefaultBuildDescriptor)buildDescriptor).isActivateToolWindowWhenFailed();

    myRootNode = new ExecutionNode(myProject, null, true, this::isCorrectThread);
    myBuildProgressRootNode = new ExecutionNode(myProject, myRootNode, true, this::isCorrectThread);
    myRootNode.setFilter(getFilter());
    myRootNode.add(myBuildProgressRootNode);

    AbstractTreeStructure treeStructure = new MyTreeStructure();
    myTreeModel = new StructureTreeModel<>(treeStructure, null, Invoker.forBackgroundThreadWithoutReadAction(this), this);
    AsyncTreeModel asyncTreeModel = new AsyncTreeModel(myTreeModel, this);
    asyncTreeModel.addTreeModelListener(new ExecutionNodeAutoExpandingListener());
    myTree = initTree(asyncTreeModel);

    JPanel myContentPanel = new JPanel();
    myContentPanel.setLayout(new CardLayout());
    myContentPanel.add(ScrollPaneFactory.createScrollPane(myTree, SideBorder.NONE), TREE);

    myPanel.setLayout(new BorderLayout());
    OnePixelSplitter myThreeComponentsSplitter = new OnePixelSplitter(SPLITTER_PROPERTY, 0.33f);
    myThreeComponentsSplitter.setFirstComponent(myContentPanel);
    List<Filter> filters = myBuildDescriptor.getExecutionFilters();
    myConsoleViewHandler = new ConsoleViewHandler(myProject, myTree, myBuildProgressRootNode, this,
                                                  executionConsole, filters, buildViewSettingsProvider);
    myThreeComponentsSplitter.setSecondComponent(myConsoleViewHandler.getComponent());
    myPanel.add(myThreeComponentsSplitter, BorderLayout.CENTER);
    BuildTreeFilters.install(this);
    myOccurrenceNavigatorSupport = new ProblemOccurrenceNavigatorSupport(myTree);
  }

  private boolean isCorrectThread() {
    if (myTreeModel != null) {
      return myTreeModel.getInvoker().isValidThread();
    }
    return true;
  }

  private void installContextMenu() {
    invokeLaterIfNeeded(() -> {
      final DefaultActionGroup rerunActionGroup = new DefaultActionGroup();
      List<AnAction> restartActions = myBuildDescriptor.getRestartActions();
      rerunActionGroup.addAll(restartActions);
      if (!restartActions.isEmpty()) {
        rerunActionGroup.addSeparator();
      }

      final DefaultActionGroup sourceActionGroup = new DefaultActionGroup();
      EditSourceAction edit = new EditSourceAction();
      ActionUtil.copyFrom(edit, "EditSource");
      sourceActionGroup.add(edit);
      DefaultActionGroup filteringActionsGroup = BuildTreeFilters.createFilteringActionsGroup(this);
      final DefaultActionGroup navigationActionGroup = new DefaultActionGroup();
      final CommonActionsManager actionsManager = CommonActionsManager.getInstance();
      final AnAction prevAction = actionsManager.createPrevOccurenceAction(this);
      navigationActionGroup.add(prevAction);
      final AnAction nextAction = actionsManager.createNextOccurenceAction(this);
      navigationActionGroup.add(nextAction);

      myTree.addMouseListener(new PopupHandler() {
        @Override
        public void invokePopup(Component comp, int x, int y) {
          final DefaultActionGroup group = new DefaultActionGroup();
          group.addAll(rerunActionGroup);
          group.addAll(sourceActionGroup);
          group.addSeparator();
          ExecutionNode[] selectedNodes = getSelectedNodes();
          if (selectedNodes.length == 1) {
            ExecutionNode selectedNode = selectedNodes[0];
            List<AnAction> contextActions = myBuildDescriptor.getContextActions(selectedNode);
            if (!contextActions.isEmpty()) {
              group.addAll(contextActions);
              group.addSeparator();
            }
          }
          group.addAll(filteringActionsGroup);
          group.addSeparator();
          group.addAll(navigationActionGroup);
          ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu("BuildView", group);
          popupMenu.setTargetComponent(myTree);
          JPopupMenu menu = popupMenu.getComponent();
          menu.show(comp, x, y);
        }
      });
    });
  }

  @Override
  public void clear() {
    myTreeModel.getInvoker().runOrInvokeLater(() -> {
      getRootElement().removeChildren();
      nodesMap.clear();
      myConsoleViewHandler.clear();
    });
    scheduleUpdate(getRootElement(), true);
  }

  @Override
  public boolean isFilteringEnabled() {
    return true;
  }

  @Override
  public @NotNull Predicate<ExecutionNode> getFilter() {
    return executionNode -> executionNode == getBuildProgressRootNode() ||
                            executionNode.isRunning() ||
                            executionNode.isFailed() ||
                            myNodeFilters.stream().anyMatch(predicate -> predicate.test(executionNode));
  }

  @Override
  public void addFilter(@NotNull Predicate<ExecutionNode> executionTreeFilter) {
    myNodeFilters.add(executionTreeFilter);
    updateFilter();
  }

  @Override
  public void removeFilter(@NotNull Predicate<ExecutionNode> filter) {
    myNodeFilters.remove(filter);
    updateFilter();
  }

  @Override
  public boolean contains(@NotNull Predicate<ExecutionNode> filter) {
    return myNodeFilters.contains(filter);
  }

  private void updateFilter() {
    ExecutionNode rootElement = getRootElement();
    myTreeModel.getInvoker().runOrInvokeLater(() -> {
      rootElement.setFilter(getFilter());
      scheduleUpdate(rootElement, true);
    });
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

  private @Nullable ExecutionNode getOrMaybeCreateParentNode(@NotNull BuildEvent event) {
    ExecutionNode parentNode = event.getParentId() == null ? null : nodesMap.get(event.getParentId());
    if (event instanceof MessageEvent) {
      parentNode = createMessageParentNodes((MessageEvent)event, parentNode);
      if (parentNode != null) {
        scheduleUpdate(parentNode, true); // To update its parent.
      }
    }
    return parentNode;
  }

  private void onEventInternal(@NotNull Object buildId, @NotNull BuildEvent event) {
    SmartHashSet<ExecutionNode> structureChanged = new SmartHashSet<>();
    final ExecutionNode parentNode = getOrMaybeCreateParentNode(event);
    final Object eventId = event.getId();
    ExecutionNode currentNode = nodesMap.get(eventId);
    ExecutionNode buildProgressRootNode = getBuildProgressRootNode();
    Runnable selectErrorNodeTask = null;
    boolean isMessageEvent = event instanceof MessageEvent;
    if (event instanceof StartEvent || isMessageEvent) {
      if (currentNode == null) {
        if (event instanceof DuplicateMessageAware) {
          if (myFinishedBuildEventReceived.get()) {
            if (parentNode != null &&
                parentNode.findFirstChild(node -> event.getMessage().equals(node.getName())) != null) {
              return;
            }
          }
          else {
            myDeferredEvents.add(event);
            return;
          }
        }
        if (event instanceof StartBuildEvent) {
          currentNode = buildProgressRootNode;
          installContextMenu();
          currentNode.setTitle(myBuildDescriptor.getTitle());
        }
        else {
          currentNode = new ExecutionNode(myProject, parentNode, false, this::isCorrectThread);

          if (isMessageEvent) {
            currentNode.setAlwaysLeaf(event instanceof FileMessageEvent);
            //noinspection CastConflictsWithInstanceof
            MessageEvent messageEvent = (MessageEvent)event;
            currentNode.setStartTime(messageEvent.getEventTime());
            addIfNotNull(structureChanged, currentNode.setEndTime(messageEvent.getEventTime()));
            Navigatable messageEventNavigatable = messageEvent.getNavigatable(myProject);
            currentNode.setNavigatable(messageEventNavigatable);
            MessageEventResult messageEventResult = messageEvent.getResult();
            addIfNotNull(structureChanged, currentNode.setResult(messageEventResult));

            if (messageEventResult instanceof FailureResult) {
              for (Failure failure : ((FailureResult)messageEventResult).getFailures()) {
                selectErrorNodeTask =
                  selectErrorNodeTask != null ? selectErrorNodeTask : showErrorIfFirst(currentNode, failure.getNavigatable());
              }
            }
            if (messageEvent.getKind() == MessageEvent.Kind.ERROR) {
              selectErrorNodeTask =
                selectErrorNodeTask != null ? selectErrorNodeTask : showErrorIfFirst(currentNode, messageEventNavigatable);
            }

            if (parentNode != null) {
              if (parentNode != buildProgressRootNode) {
                myConsoleViewHandler.addOutput(parentNode, buildId, event);
                myConsoleViewHandler.addOutput(parentNode, "\n", true);
              }
              reportMessageKind(messageEvent, parentNode);
            }
            myConsoleViewHandler.addOutput(currentNode, buildId, event);
          }
          if (parentNode != null) {
            structureChanged.add(parentNode);
            parentNode.add(currentNode);
          }
        }
        nodesMap.put(eventId, currentNode);
      }
      else {
        LOG.warn("start event id collision found:" + eventId + ", was also in node: " + currentNode.getTitle());
        return;
      }
    }
    else {
      boolean isProgress = event instanceof ProgressBuildEvent;
      currentNode = nodesMap.get(eventId);
      if (currentNode == null) {
        if (isProgress) {
          currentNode = new ExecutionNode(myProject, parentNode, parentNode == buildProgressRootNode, this::isCorrectThread);
          nodesMap.put(eventId, currentNode);
          if (parentNode != null) {
            structureChanged.add(parentNode);
            parentNode.add(currentNode);
          }
        }
        else if (event instanceof OutputBuildEvent && parentNode != null) {
          myConsoleViewHandler.addOutput(parentNode, buildId, event);
        }
      }

      if (isProgress) {
        ProgressBuildEvent progressBuildEvent = (ProgressBuildEvent)event;
        long total = progressBuildEvent.getTotal();
        long progress = progressBuildEvent.getProgress();
        if (currentNode == myBuildProgressRootNode) {
          myConsoleViewHandler.updateProgressBar(total, progress);
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
      EventResult result = ((FinishEvent)event).getResult();
      if (result instanceof DerivedResult) {
        result = calculateDerivedResult((DerivedResult)result, currentNode);
      }
      addIfNotNull(structureChanged, currentNode.setResult(result));
      addIfNotNull(structureChanged, currentNode.setEndTime(event.getEventTime()));
      SkippedResult skippedResult = new SkippedResultImpl();
      finishChildren(structureChanged, currentNode, skippedResult);
      if (result instanceof FailureResult) {
        for (Failure failure : ((FailureResult)result).getFailures()) {
          Runnable task = addChildFailureNode(currentNode, failure, event.getMessage(), structureChanged);
          if (selectErrorNodeTask == null) selectErrorNodeTask = task;
        }
      }
    }

    if (event instanceof FinishBuildEvent) {
      myFinishedBuildEventReceived.set(true);
      String aHint = event.getHint();
      String time = DateFormatUtil.formatDateTime(event.getEventTime());
      aHint = aHint == null ? "at " + time : aHint + " at " + time;
      currentNode.setHint(aHint);
      myDeferredEvents.forEach(buildEvent -> onEventInternal(buildId, buildEvent));
      if (myConsoleViewHandler.myExecutionNode == null) {
        invokeLater(() -> myConsoleViewHandler.setNode(buildProgressRootNode));
      }
      myConsoleViewHandler.stopProgressBar();
    }
    if (structureChanged.isEmpty()) {
      scheduleUpdate(currentNode, false);
    }
    else {
      for (ExecutionNode node : structureChanged) {
        scheduleUpdate(node, true);
      }
    }
    if (selectErrorNodeTask != null) {
      myExpandedFirstMessage.set(true);
      Runnable finalSelectErrorTask = selectErrorNodeTask;
      myTreeModel.invalidate(getRootElement(), true).onProcessed(p -> finalSelectErrorTask.run());
    } else {
      if (isMessageEvent && myExpandedFirstMessage.compareAndSet(false, true)) {
        ExecutionNode finalCurrentNode = currentNode;
        myTreeModel.invalidate(getRootElement(), false).onProcessed(p -> {
          TreeUtil.promiseMakeVisible(myTree, visitor(finalCurrentNode));
        });
      }
    }
  }

  @ApiStatus.Internal
  @TestOnly
  public @Nullable String getSelectedNodeConsoleText() {
    ExecutionConsole console = myConsoleViewHandler.getCurrentConsole();
    if (console instanceof ConsoleViewImpl) {
      ((ConsoleViewImpl)console).flushDeferredText();
      return ((ConsoleViewImpl)console).getText();
    }
    if (myConsoleViewHandler.myView.isViewVisible(ConsoleViewHandler.EMPTY_CONSOLE_NAME)) {
      return "";
    }
    return null;
  }

  private static EventResult calculateDerivedResult(DerivedResult result, ExecutionNode node) {
    if (node.getResult() != null) {
      return node.getResult(); // if another thread set result for child
    }
    if (node.isFailed()) {
      return result.createFailureResult();
    }

    return result.createDefaultResult();
  }

  private void reportMessageKind(@NotNull MessageEvent messageEvent, @NotNull ExecutionNode parentNode) {
    final MessageEvent.Kind eventKind = messageEvent.getKind();
    if (eventKind == MessageEvent.Kind.ERROR || eventKind == MessageEvent.Kind.WARNING || eventKind == MessageEvent.Kind.INFO) {
      ExecutionNode executionNode = parentNode;
      do {
        ExecutionNode updatedRoot = executionNode.reportChildMessageKind(eventKind);
        if (updatedRoot != null) {
          scheduleUpdate(updatedRoot, true);
        }
        else {
          scheduleUpdate(executionNode, false);
        }
      }
      while ((executionNode = executionNode.getParent()) != null);
      scheduleUpdate(getRootElement(), false);
    }
  }

  private @Nullable Runnable showErrorIfFirst(@NotNull ExecutionNode node, @Nullable Navigatable navigatable) {
    if (myShownFirstError.compareAndSet(false, true)) {
      return () -> {
        TreeUtil.promiseSelect(myTree, visitor(node));
        if (myFocusFirstError && navigatable != null && navigatable != NonNavigatable.INSTANCE) {
          ApplicationManager.getApplication()
            .invokeLater(() -> navigatable.navigate(true), ModalityState.defaultModalityState(), myProject.getDisposed());
        }
      };
    }
    return null;
  }

  @Override
  public boolean hasNextOccurence() {
    return myOccurrenceNavigatorSupport.hasNextOccurence();
  }

  @Override
  public boolean hasPreviousOccurence() {
    return myOccurrenceNavigatorSupport.hasPreviousOccurence();
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    return myOccurrenceNavigatorSupport.goNextOccurence();
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    return myOccurrenceNavigatorSupport.goPreviousOccurence();
  }

  @Override
  public @NotNull String getNextOccurenceActionName() {
    return myOccurrenceNavigatorSupport.getNextOccurenceActionName();
  }

  @Override
  public @NotNull String getPreviousOccurenceActionName() {
    return myOccurrenceNavigatorSupport.getPreviousOccurenceActionName();
  }

  private static @NotNull TreeVisitor visitor(@NotNull ExecutionNode executionNode) {
    TreePath treePath = TreePathUtil.pathToCustomNode(executionNode, node -> node.getParent());
    return new TreeVisitor.ByTreePath<>(treePath, o -> (ExecutionNode)TreeUtil.getUserObject(o));
  }

  private @Nullable Runnable addChildFailureNode(@NotNull ExecutionNode parentNode,
                                                 @NotNull Failure failure,
                                                 @NotNull String defaultFailureMessage,
                                                 @NotNull Set<ExecutionNode> strcutreChanged) {
    String text = chooseNotNull(failure.getDescription(), failure.getMessage());
    if (text == null && failure.getError() != null) {
      text = failure.getError().getMessage();
    }
    if (text == null) {
      text = defaultFailureMessage;
    }
    String failureNodeName = getMessageTitle(text);
    ExecutionNode failureNode = parentNode.findFirstChild(executionNode -> failureNodeName.equals(executionNode.getName()));
    if (failureNode == null) {
      failureNode = new ExecutionNode(myProject, parentNode, true, this::isCorrectThread);
      failureNode.setName(failureNodeName);
      parentNode.add(failureNode);
    }
    if (failure.getNavigatable() == null) {
      failureNode.setNavigatable(failure.getNavigatable());
    }

    List<Failure> failures;
    EventResult result = failureNode.getResult();
    if (result instanceof FailureResult) {
      failures = new ArrayList<>(((FailureResult)result).getFailures());
      failures.add(failure);
    }
    else {
      failures = Collections.singletonList(failure);
    }
    ExecutionNode updatedRoot = failureNode.setResult(new FailureResultImpl(failures));
    if (updatedRoot == null) {
      updatedRoot = parentNode;
    }
    strcutreChanged.add(updatedRoot);
    myConsoleViewHandler.addOutput(failureNode, failure);
    return showErrorIfFirst(failureNode, failure.getNavigatable());
  }

  private static void finishChildren(@NotNull SmartHashSet<ExecutionNode> structureChanged,
                                     @NotNull ExecutionNode node,
                                     @NotNull EventResult result) {
    List<ExecutionNode> childList = node.getChildList();
    if (childList.isEmpty()) return;
    // Make a copy of the list since child.setResult may remove items from the collection.
    for (ExecutionNode child : new ArrayList<>(childList)) {
      if (!child.isRunning()) {
        continue;
      }
      finishChildren(structureChanged, child, result);
      addIfNotNull(structureChanged, child.setResult(result));
    }
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

  @Override
  public AnAction @NotNull [] createConsoleActions() {
    return AnAction.EMPTY_ARRAY;
  }

  @Override
  public void allowHeavyFilters() {
  }

  @Override
  public @NotNull JComponent getComponent() {
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
  public void onEvent(@NotNull Object buildId, @NotNull BuildEvent event) {
    myTreeModel.getInvoker().invoke(() -> onEventInternal(buildId, event));
  }

  void scheduleUpdate(ExecutionNode executionNode, boolean parentStructureChanged) {
    ExecutionNode node = (executionNode.getParent() == null || !parentStructureChanged) ? executionNode : executionNode.getParent();
    myTreeModel.invalidate(node, parentStructureChanged);
  }

  private ExecutionNode createMessageParentNodes(MessageEvent messageEvent, ExecutionNode parentNode) {
    Object messageEventParentId = messageEvent.getParentId();
    if (messageEventParentId == null) return null;

    if (messageEvent instanceof FileMessageEvent) {
      FilePosition filePosition = ((FileMessageEvent)messageEvent).getFilePosition();
      String filePath = FileUtil.toSystemIndependentName(filePosition.getFile().getPath());
      String parentsPath = "";

      String relativePath = FileUtil.getRelativePath(myWorkingDir, filePath, '/');
      if (relativePath != null) {
        if (relativePath.equals(".")) {
          return parentNode;
        }
        parentsPath = myWorkingDir;
      }

      relativePath = isEmpty(parentsPath) ? filePath : getRelativePath(parentsPath, filePath);
      Path path = Paths.get(relativePath);
      String nodeName = path.getFileName().toString();
      Path pathParent = path.getParent();
      String pathHint = pathParent == null ? null : pathParent.toString();
      parentNode = getOrCreateMessagesNode(messageEvent, filePath, parentNode, nodeName, pathHint,
                                           () -> {
                                             VirtualFile file = VfsUtil.findFileByIoFile(filePosition.getFile(), false);
                                             if (file != null) {
                                               return file.getFileType().getIcon();
                                             }
                                             return null;
                                           }, messageEvent.getNavigatable(myProject), nodesMap, myProject);
    }
    return parentNode;
  }

  private static String getRelativePath(@NotNull String basePath, @NotNull String filePath) {
    String path = ObjectUtils.notNull(FileUtil.getRelativePath(basePath, filePath, '/'), filePath);
    File userHomeDir = new File(SystemProperties.getUserHome());
    if (path.startsWith("..") && FileUtil.isAncestor(userHomeDir, new File(filePath), true)) {
      return FileUtil.getLocationRelativeToUserHome(filePath, false);
    }
    return path;
  }

  public void hideRootNode() {
    invokeLaterIfNeeded(() -> {
      if (myTree != null) {
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);
      }
    });
  }

  @Override
  public @Nullable Object getData(@NotNull String dataId) {
    if (PlatformDataKeys.HELP_ID.is(dataId)) return "reference.build.tool.window";
    if (CommonDataKeys.PROJECT.is(dataId)) return myProject;
    if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) return extractSelectedNodesNavigatables();
    if (CommonDataKeys.NAVIGATABLE.is(dataId)) return extractSelectedNodeNavigatable();
    return null;
  }

  private @Nullable Object extractSelectedNodeNavigatable() {
    TreePath selectedPath = TreeUtil.getSelectedPathIfOne(myTree);
    if (selectedPath == null) return null;
    DefaultMutableTreeNode node = ObjectUtils.tryCast(selectedPath.getLastPathComponent(), DefaultMutableTreeNode.class);
    if (node == null) return null;
    ExecutionNode executionNode = ObjectUtils.tryCast(node.getUserObject(), ExecutionNode.class);
    if (executionNode == null) return null;
    List<Navigatable> navigatables = executionNode.getNavigatables();
    if (navigatables.size() != 1) return null;
    return navigatables.get(0);
  }

  private Object extractSelectedNodesNavigatables() {
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

  @ApiStatus.Internal
  public JTree getTree() {
    return myTree;
  }

  private static Tree initTree(@NotNull AsyncTreeModel model) {
    Tree tree = new Tree(model);
    tree.setLargeModel(true);
    ComponentUtil.putClientProperty(tree, ANIMATION_IN_RENDERER_ALLOWED, true);
    ComponentUtil.putClientProperty(tree, AUTO_EXPAND_ALLOWED, false);
    tree.setRootVisible(false);
    EditSourceOnDoubleClickHandler.install(tree);
    EditSourceOnEnterKeyHandler.install(tree, null);
    new TreeSpeedSearch(tree).setComparator(new SpeedSearchComparator(false));
    TreeUtil.installActions(tree);
    tree.setCellRenderer(new MyNodeRenderer());
    tree.putClientProperty(SHRINK_LONG_RENDERER, true);
    return tree;
  }

  private @NotNull ExecutionNode getOrCreateMessagesNode(MessageEvent messageEvent,
                                                         String nodeId,
                                                         ExecutionNode parentNode,
                                                         String nodeName,
                                                         @Nullable String hint,
                                                         @Nullable Supplier<? extends Icon> iconProvider,
                                                         @Nullable Navigatable navigatable,
                                                         Map<Object, ExecutionNode> nodesMap,
                                                         Project project) {
    ExecutionNode node = nodesMap.get(nodeId);
    if (node == null) {
      node = new ExecutionNode(project, parentNode, false, this::isCorrectThread);
      node.setName(nodeName);
      if (hint != null) {
        node.setHint(hint);
      }
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

  @ApiStatus.Internal
  public Promise<?> invokeLater(@NotNull Runnable task) {
    return myTreeModel.getInvoker().invokeLater(task);
  }

  private static class ConsoleViewHandler implements Disposable {
    private static final String EMPTY_CONSOLE_NAME = "empty";
    private final Project myProject;
    private final JPanel myPanel;
    private final CompositeView<ExecutionConsole> myView;
    private final AtomicReference<String> myNodeConsoleViewName = new AtomicReference<>();
    private final Map<String, List<Consumer<BuildTextConsoleView>>> deferredNodeOutput = new ConcurrentHashMap<>();
    private final @NotNull BuildViewSettingsProvider myViewSettingsProvider;
    private @Nullable ExecutionNode myExecutionNode;
    private @NotNull final List<Filter> myExecutionConsoleFilters;
    private final BuildProgressStripe myPanelWithProgress;

    ConsoleViewHandler(@NotNull Project project,
                       @NotNull Tree tree,
                       @NotNull ExecutionNode buildProgressRootNode,
                       @NotNull Disposable parentDisposable,
                       @Nullable ExecutionConsole executionConsole,
                       @NotNull List<Filter> executionConsoleFilters,
                       @NotNull BuildViewSettingsProvider buildViewSettingsProvider) {
      myProject = project;
      myPanel = new NonOpaquePanel(new BorderLayout());
      myPanelWithProgress = new BuildProgressStripe(myPanel, parentDisposable, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
      myViewSettingsProvider = buildViewSettingsProvider;
      myExecutionConsoleFilters = executionConsoleFilters;
      Disposer.register(parentDisposable, this);
      myView = new CompositeView<ExecutionConsole>(null) {
        @Override
        public void addView(@NotNull ExecutionConsole view, @NotNull String viewName) {
          super.addView(view, viewName);
          removeScrollBorder(view.getComponent());
        }
      };
      Disposer.register(this, myView);
      if (executionConsole != null && buildViewSettingsProvider.isSideBySideView()) {
        String nodeConsoleViewName = getNodeConsoleViewName(buildProgressRootNode);
        myView.addViewAndShowIfNeeded(executionConsole, nodeConsoleViewName, true);
        myNodeConsoleViewName.set(nodeConsoleViewName);
      }
      ConsoleView emptyConsole = new ConsoleViewImpl(project, GlobalSearchScope.EMPTY_SCOPE, true, false);
      myView.addView(emptyConsole, EMPTY_CONSOLE_NAME);
      if (!buildViewSettingsProvider.isSideBySideView()) {
        myPanel.setVisible(false);
      }
      JComponent consoleComponent = emptyConsole.getComponent();
      consoleComponent.setFocusable(true);
      myPanel.add(myView.getComponent(), BorderLayout.CENTER);
      DefaultActionGroup consoleActionsGroup = new DefaultActionGroup();
      consoleActionsGroup.add(new ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
        @Override
        protected @Nullable Editor getEditor(@NotNull AnActionEvent e) {
          return ConsoleViewHandler.this.getEditor();
        }
      });
      consoleActionsGroup.add(new ScrollEditorToTheEndAction(this));
      final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("BuildConsole", consoleActionsGroup, false);
      myPanel.add(toolbar.getComponent(), BorderLayout.EAST);
      tree.addTreeSelectionListener(e -> {
        TreePath path = e.getPath();
        if (path == null || !e.isAddedPath()) {
          return;
        }
        TreePath selectionPath = tree.getSelectionPath();
        setNode(selectionPath != null ? (DefaultMutableTreeNode)selectionPath.getLastPathComponent() : null);
      });
    }

    private void updateProgressBar(long total, long progress) {
      myPanelWithProgress.updateProgress(total, progress);
    }

    private @Nullable ExecutionConsole getCurrentConsole() {
      String nodeConsoleViewName = myNodeConsoleViewName.get();
      if (nodeConsoleViewName == null) return null;
      return myView.getView(nodeConsoleViewName);
    }

    private @Nullable Editor getEditor() {
      ExecutionConsole console = getCurrentConsole();
      if (console instanceof ConsoleViewImpl) {
        return ((ConsoleViewImpl)console).getEditor();
      }
      return null;
    }

    private boolean setNode(@NotNull ExecutionNode node) {
      String nodeConsoleViewName = getNodeConsoleViewName(node);
      myNodeConsoleViewName.set(nodeConsoleViewName);
      ExecutionConsole view = myView.getView(nodeConsoleViewName);
      if (view != null) {
        List<Consumer<BuildTextConsoleView>> deferredOutput = deferredNodeOutput.get(nodeConsoleViewName);
        if (view instanceof BuildTextConsoleView && deferredOutput != null && !deferredOutput.isEmpty()) {
          deferredNodeOutput.remove(nodeConsoleViewName);
          deferredOutput.forEach(consumer -> consumer.accept((BuildTextConsoleView)view));
        }
        myView.showView(nodeConsoleViewName, false);
        myPanel.setVisible(true);
        return true;
      }

      List<Consumer<BuildTextConsoleView>> deferredOutput = deferredNodeOutput.get(nodeConsoleViewName);
      if (deferredOutput != null && !deferredOutput.isEmpty()) {
        BuildTextConsoleView textConsoleView = new BuildTextConsoleView(myProject, true, myExecutionConsoleFilters);
        deferredNodeOutput.remove(nodeConsoleViewName);
        deferredOutput.forEach(consumer -> consumer.accept(textConsoleView));
        myView.addView(textConsoleView, nodeConsoleViewName);
        myView.showView(nodeConsoleViewName, false);
      }
      else if (myViewSettingsProvider.isSideBySideView()) {
        myView.showView(EMPTY_CONSOLE_NAME, false);
        return true;
      }

      if (!myViewSettingsProvider.isSideBySideView()) {
        EventResult eventResult = node.getResult();
        BuildTextConsoleView taskOutputView = new BuildTextConsoleView(myProject, true, myExecutionConsoleFilters);
        boolean hasChanged = taskOutputView.appendEventResult(eventResult);
        if (!hasChanged) return false;

        taskOutputView.scrollTo(0);
        myView.addView(taskOutputView, nodeConsoleViewName);
        myView.showView(nodeConsoleViewName, false);
        myPanel.setVisible(true);
      }
      return true;
    }

    private void addOutput(@NotNull ExecutionNode node, @NotNull String text, boolean stdOut) {
      addOutput(node, view -> view.append(text, stdOut));
    }

    private void addOutput(@NotNull ExecutionNode node, @NotNull Object buildId, BuildEvent event) {
      addOutput(node, view -> view.onEvent(buildId, event));
    }

    private void addOutput(@NotNull ExecutionNode node, Failure failure) {
      addOutput(node, view -> view.append(failure));
    }

    private void addOutput(@NotNull ExecutionNode node, Consumer<BuildTextConsoleView> consumer) {
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

    @Override
    public void dispose() {
      deferredNodeOutput.clear();
    }

    private void stopProgressBar() {
      myPanelWithProgress.stopLoading();
    }

    private static @NotNull String getNodeConsoleViewName(@NotNull ExecutionNode node) {
      return String.valueOf(System.identityHashCode(node));
    }

    private void setNode(@Nullable DefaultMutableTreeNode node) {
      if (myProject.isDisposed()) return;
      if (node == null || node.getUserObject() == myExecutionNode) return;
      if (node.getUserObject() instanceof ExecutionNode) {
        myExecutionNode = (ExecutionNode)node.getUserObject();
        if (setNode((ExecutionNode)node.getUserObject())) {
          return;
        }
      }

      myExecutionNode = null;
      if (myView.getView(CONSOLE_VIEW_NAME) != null/* && myViewSettingsProvider.isSideBySideView()*/) {
        myView.showView(CONSOLE_VIEW_NAME, false);
        myPanel.setVisible(true);
      }
      else {
        myPanel.setVisible(false);
      }
    }

    public JComponent getComponent() {
      return myPanelWithProgress;
    }

    public void clear() {
      myPanel.setVisible(false);
    }
  }

  private static class ProblemOccurrenceNavigatorSupport extends OccurenceNavigatorSupport {
    ProblemOccurrenceNavigatorSupport(final Tree tree) {
      super(tree);
    }

    @Override
    protected Navigatable createDescriptorForNode(@NotNull DefaultMutableTreeNode node) {
      Object userObject = node.getUserObject();
      if (!(userObject instanceof ExecutionNode)) {
        return null;
      }
      final ExecutionNode executionNode = (ExecutionNode)userObject;
      if (node.getChildCount() != 0 || !executionNode.hasWarnings() && !executionNode.isFailed()) {
        return null;
      }
      List<Navigatable> navigatables = executionNode.getNavigatables();
      if (!navigatables.isEmpty()) {
        return navigatables.get(0);
      }
      return null;
    }

    @Override
    public @NotNull String getNextOccurenceActionName() {
      return IdeBundle.message("action.next.problem");
    }

    @Override
    public @NotNull String getPreviousOccurenceActionName() {
      return IdeBundle.message("action.previous.problem");
    }
  }

  private static class ScrollEditorToTheEndAction extends ToggleAction implements DumbAware {
    private final @NotNull ConsoleViewHandler myConsoleViewHandler;

    ScrollEditorToTheEndAction(@NotNull ConsoleViewHandler handler) {
      super(ActionsBundle.message("action.EditorConsoleScrollToTheEnd.text"), null, AllIcons.RunConfigurations.Scroll_down);
      myConsoleViewHandler = handler;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      Editor editor = myConsoleViewHandler.getEditor();
      if (editor == null) return false;
      Document document = editor.getDocument();
      return document.getLineCount() == 0 || document.getLineNumber(editor.getCaretModel().getOffset()) == document.getLineCount() - 1;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      Editor editor = myConsoleViewHandler.getEditor();
      if (editor == null) return;
      if (state) {
        EditorUtil.scrollToTheEnd(editor);
      }
      else {
        int lastLine = Math.max(0, editor.getDocument().getLineCount() - 1);
        LogicalPosition currentPosition = editor.getCaretModel().getLogicalPosition();
        LogicalPosition position = new LogicalPosition(Math.max(0, Math.min(currentPosition.line, lastLine - 1)), currentPosition.column);
        editor.getCaretModel().moveToLogicalPosition(position);
      }
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

  private class MyTreeStructure extends AbstractTreeStructure {
    @Override
    public @NotNull Object getRootElement() {
      return myRootNode;
    }

    @Override
    public @NotNull Object[] getChildElements(@NotNull Object element) {
      // This .toArray() is still slow but it is called less frequently because of batching in AsyncTreeModel and process less data if
      // filters are applied.
      return ((ExecutionNode)element).getChildList().toArray();
    }

    @Override
    public @Nullable Object getParentElement(@NotNull Object element) {
      return ((ExecutionNode)element).getParent();
    }

    @Override
    public @NotNull NodeDescriptor createDescriptor(@NotNull Object element, @Nullable NodeDescriptor parentDescriptor) {
      return ((NodeDescriptor)element);
    }

    @Override
    public void commit() { }

    @Override
    public boolean hasSomethingToCommit() {
      return false;
    }

    @Override
    public boolean isAlwaysLeaf(@NotNull Object element) {
      return ((ExecutionNode)element).isAlwaysLeaf();
    }
  }

  private class ExecutionNodeAutoExpandingListener implements TreeModelListener {
    @Override
    public void treeNodesInserted(TreeModelEvent e) {
      maybeExpand(e.getTreePath());
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
      // ExecutionNode should never change its isAutoExpand state. Ignore calls and do nothing.
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
      // A removed node is not a reason to expand it parent. Ignore calls and do nothing.
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
      // We do not expect this event to happen in cases other than clearing the tree (including changing the filter).
      // Ignore calls and do nothing.
    }

    private boolean maybeExpand(TreePath path) {
      if (myTree == null || path == null) return false;
      Object last = path.getLastPathComponent();
      if (last instanceof DefaultMutableTreeNode) {
        DefaultMutableTreeNode mutableTreeNode = (DefaultMutableTreeNode)last;
        boolean expanded = false;
        Enumeration<?> children = mutableTreeNode.children();
        if (children.hasMoreElements()) {
          while (children.hasMoreElements()) {
            Object next = children.nextElement();
            if (next != null) {
              expanded = maybeExpand(path.pathByAddingChild(next)) || expanded;
            }
          }
          if (expanded) return true;
          Object lastUserObject = mutableTreeNode.getUserObject();
          if (lastUserObject instanceof ExecutionNode) {
            if (((ExecutionNode)lastUserObject).isAutoExpandNode()) {
              if (!myTree.isExpanded(path)) {
                myTree.expandPath(path);
                return true;
              }
            }
          }
        }
      }
      return false;
    }
  }
}
