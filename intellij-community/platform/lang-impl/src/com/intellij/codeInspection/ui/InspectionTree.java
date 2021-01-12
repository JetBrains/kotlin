// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ui;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.InspectionToolResultExporter;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.BatchModeDescriptorsUtil;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.ui.util.SynchronizedBidiMultiMap;
import com.intellij.ide.DataManager;
import com.intellij.ide.OccurenceNavigator;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeComparator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.SmartExpander;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.TreeCollector.TreePathRoots;
import com.intellij.ui.tree.TreePathUtil;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.OpenSourceUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.Stack;
import com.intellij.util.containers.TreeTraversal;
import com.intellij.util.ui.EdtInvocationManager;
import com.intellij.util.ui.tree.TreeModelAdapter;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.stream.Stream;

import static com.intellij.codeInspection.CommonProblemDescriptor.DESCRIPTOR_COMPARATOR;

public class InspectionTree extends Tree {
  private static final Logger LOG = Logger.getInstance(InspectionTree.class);

  private final InspectionTreeModel myModel;

  private boolean myQueueUpdate;
  private final OccurenceNavigator myOccurenceNavigator = new MyOccurrenceNavigator();
  private final InspectionResultsView myView;

  public InspectionTree(@NotNull InspectionResultsView view) {
    myView = view;
    myModel = new InspectionTreeModel();
    Disposer.register(view, myModel);
    setModel(new AsyncTreeModel(myModel, false, view));

    setCellRenderer(new InspectionTreeCellRenderer(view));
    setRootVisible(false);
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      getSelectionModel().addTreeSelectionListener(e -> {
        if (isUnderQueueUpdate()) return;
        if (!myView.isDisposed()) {
          myView.syncRightPanel();
          if (myView.isAutoScrollMode()) {
            OpenSourceUtil.openSourcesFrom(DataManager.getInstance().getDataContext(this), false);
          }
        }
      });

      EditSourceOnDoubleClickHandler.install(this);
      EditSourceOnEnterKeyHandler.install(this);
      TreeUtil.installActions(this);
      PopupHandler.installPopupHandler(this, IdeActions.INSPECTION_TOOL_WINDOW_TREE_POPUP, ActionPlaces.CODE_INSPECTION);
      new TreeSpeedSearch(this, o -> InspectionsConfigTreeComparator.getDisplayTextToSort(o.getLastPathComponent().toString()));
    }

    getModel().addTreeModelListener(new TreeModelAdapter() {
      //TODO the same as DiscoveredTestTree (see setRootVisible)
      boolean myAlreadyDone;
      @Override
      protected void process(@NotNull TreeModelEvent event, @NotNull EventType type) {
        if (!myAlreadyDone && getSelectionCount() == 0) {
          myAlreadyDone = true;
          EdtInvocationManager.getInstance().invokeLater(() -> {
            expandPath(new TreePath(myModel.getRoot()));
            SmartExpander.installOn(InspectionTree.this);
            if (!myView.isDisposed()) {
              myView.syncRightPanel();
            }
          });
        }
      }
    });
  }

  public InspectionTreeNode getRoot() {
    return myModel.getRoot();
  }

  public InspectionTreeModel getInspectionTreeModel() {
    return myModel;
  }

  public void setQueueUpdate(boolean queueUpdate) {
    myQueueUpdate = queueUpdate;
  }

  public boolean isUnderQueueUpdate() {
    return myQueueUpdate;
  }

  public void removeAllNodes() {
    myModel.clearTree();
  }

  public String @Nullable [] getSelectedGroupPath() {
    TreePath commonPath = TreePathUtil.findCommonAncestor(getSelectionPaths());
    if (commonPath == null) return null;
    for (Object n : commonPath.getPath()) {
      if (n instanceof InspectionGroupNode) {
        return getGroupPath((InspectionGroupNode)n);
      }
    }
    return null;
  }

  @Nullable
  public InspectionToolWrapper getSelectedToolWrapper(boolean allowDummy) {
    InspectionProfileImpl profile = myView.getCurrentProfile();
    String singleToolName = profile.getSingleTool();
    final TreePath[] paths = getSelectionPaths();
    if (paths == null) {
      if (singleToolName != null) {
        InspectionToolWrapper tool = profile.getInspectionTool(singleToolName, myView.getProject());
        LOG.assertTrue(tool != null);
        return tool;
      }
      return null;
    }
    InspectionToolWrapper resultWrapper = null;
    for (TreePath path : paths) {
      Object[] nodes = path.getPath();
      for (int j = nodes.length - 1; j >= 0; j--) {
        Object node = nodes[j];
        if (node instanceof InspectionGroupNode) {
          return null;
        }
        InspectionToolWrapper wrapper = null;
        if (node instanceof InspectionNode) {
          wrapper = ((InspectionNode)node).getToolWrapper();
        } else if (node instanceof SuppressableInspectionTreeNode) {
          wrapper = ((SuppressableInspectionTreeNode)node).getPresentation().getToolWrapper();
        }
        if (wrapper == null || !allowDummy && getContext().getPresentation(wrapper).isDummy()) {
          continue;
        }
        if (resultWrapper == null) {
          resultWrapper = wrapper;
        }
        else if (resultWrapper != wrapper) {
          return null;
        }
        break;
      }
    }

    if (resultWrapper == null && singleToolName != null) {
      InspectionToolWrapper tool = profile.getInspectionTool(singleToolName, myView.getProject());
      LOG.assertTrue(tool != null);
      return tool;
    }

    return resultWrapper;
  }

  @Override
  public String getToolTipText(MouseEvent e) {
    TreePath path = getPathForLocation(e.getX(), e.getY());
    if (path == null) return null;
    Object lastComponent = path.getLastPathComponent();
    if (!(lastComponent instanceof ProblemDescriptionNode)) return null;
    return ((ProblemDescriptionNode)lastComponent).getToolTipText();
  }

  @Nullable
  public RefEntity getCommonSelectedElement() {
    final Object node = getCommonSelectedNode();
    return node instanceof RefElementNode ? ((RefElementNode)node).getElement() : null;
  }

  @Nullable
  private Object getCommonSelectedNode() {
    final TreePath[] paths = getSelectionPaths();
    if (paths == null) return null;
    final Object[][] resolvedPaths = new Object[paths.length][];
    for (int i = 0; i < paths.length; i++) {
      TreePath path = paths[i];
      resolvedPaths[i] = path.getPath();
    }

    Object currentCommonNode = null;
    for (int i = 0; i < resolvedPaths[0].length; i++) {
      final Object currentNode = resolvedPaths[0][i];
      for (int j = 1; j < resolvedPaths.length; j++) {
        final Object o = resolvedPaths[j][i];
        if (!o.equals(currentNode)) {
          return currentCommonNode;
        }
      }
      currentCommonNode = currentNode;
    }
    return currentCommonNode;
  }

  public RefEntity @NotNull [] getSelectedElements() {
    TreePath[] selectionPaths = getSelectionPaths();
    if (selectionPaths != null) {
      InspectionToolWrapper toolWrapper = getSelectedToolWrapper(true);
      if (toolWrapper == null) return RefEntity.EMPTY_ELEMENTS_ARRAY;

      Set<RefEntity> result = new LinkedHashSet<>();
      for (TreePath selectionPath : selectionPaths) {
        final InspectionTreeNode node = (InspectionTreeNode)selectionPath.getLastPathComponent();
        addElementsInNode(node, result);
      }
      return ArrayUtil.reverseArray(result.toArray(RefEntity.EMPTY_ELEMENTS_ARRAY));
    }
    return RefEntity.EMPTY_ELEMENTS_ARRAY;
  }

  public OccurenceNavigator getOccurenceNavigator() {
    return myOccurenceNavigator;
  }

  public void selectNode(InspectionTreeNode node) {
    TreePath path = getPathFor(node);
    TreeUtil.selectPath(this, path);
  }

  private static void addElementsInNode(InspectionTreeNode node, Set<? super RefEntity> out) {
    if (!node.isValid()) return;
    if (node instanceof RefElementNode) {
      final RefEntity element = ((RefElementNode)node).getElement();
      out.add(element);
    }
    if (node instanceof ProblemDescriptionNode) {
      final RefEntity element = ((ProblemDescriptionNode)node).getElement();
      out.add(element);
    }

    for (InspectionTreeNode child : node.getChildren()) {
      addElementsInNode(child, out);
    }
  }

  public CommonProblemDescriptor @NotNull [] getAllValidSelectedDescriptors() {
    return BatchModeDescriptorsUtil.flattenDescriptors(getSelectedDescriptorPacks(false, null, true, null));
  }

  public CommonProblemDescriptor @NotNull [] getSelectedDescriptors() {
    return BatchModeDescriptorsUtil.flattenDescriptors(getSelectedDescriptorPacks(false, null, false, null));
  }

  @NotNull
  public List<CommonProblemDescriptor[]> getSelectedDescriptorPacks(boolean sortedByPosition,
                                                                    @Nullable Set<? super VirtualFile> readOnlyFilesSink,
                                                                    boolean allowResolved,
                                                                    TreePath[] paths) {
    if (paths == null) {
      ApplicationManager.getApplication().assertIsDispatchThread();
      paths = getSelectionPaths();
    }
    if (paths == null) return Collections.emptyList();
    // key can be node or VirtualFile (if problem descriptor node parent is a file/member RefElementNode).
    MultiMap<Object, CommonProblemDescriptor> parentToChildNode = new MultiMap<>();
    //TODO expected thread
    TreeTraversal.PLAIN_BFS.traversal(
      ContainerUtil.map(paths, p -> (InspectionTreeNode)p.getLastPathComponent()),
      (InspectionTreeNode n) -> myModel.getChildren(n))
      .filter(ProblemDescriptionNode.class)
      .filter(node -> node.getDescriptor() != null && isNodeValidAndIncluded(node, allowResolved))
      .consumeEach(node -> {
        Object key = getVirtualFileOrEntity(node.getElement());
        parentToChildNode.putValue(key, node.getDescriptor());
      });
    final List<CommonProblemDescriptor[]> descriptors = new ArrayList<>();
    for (Map.Entry<Object, Collection<CommonProblemDescriptor>> entry : parentToChildNode.entrySet()) {
      Object key = entry.getKey();
      if (readOnlyFilesSink != null && key instanceof VirtualFile && !((VirtualFile)key).isWritable()) {
        readOnlyFilesSink.add((VirtualFile)key);
      }
      Stream<CommonProblemDescriptor> stream = entry.getValue().stream();
      if (sortedByPosition) {
        stream = stream.sorted(DESCRIPTOR_COMPARATOR);
      }
      descriptors.add(stream.toArray(CommonProblemDescriptor.ARRAY_FACTORY::create));
    }

    return descriptors;
  }

  @Override
  public TreePath @Nullable [] getSelectionPaths() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return super.getSelectionPaths();
  }

  @NotNull
  InspectionTreeNode getToolProblemsRootNode(@NotNull InspectionToolWrapper toolWrapper,
                                             @NotNull HighlightDisplayLevel errorLevel,
                                             boolean groupedBySeverity,
                                             boolean isSingleInspectionRun) {
    InspectionTreeNode parent = getToolParentNode(toolWrapper, errorLevel, groupedBySeverity, isSingleInspectionRun);
    if (isSingleInspectionRun) {
      return parent;
    }
    return myModel.createInspectionNode(toolWrapper, myView.getCurrentProfile(), parent);
  }

  @NotNull
  private InspectionTreeNode getToolParentNode(@NotNull InspectionToolWrapper toolWrapper,
                                               @NotNull HighlightDisplayLevel errorLevel,
                                               boolean groupedBySeverity,
                                               boolean isSingleInspectionRun) {
    //synchronize
    if (!groupedBySeverity && isSingleInspectionRun) {
      return myModel.getRoot();
    }

    InspectionTreeNode currentNode = groupedBySeverity
                                     ? myModel.createSeverityGroupNode(myView.getCurrentProfile().getProfileManager().getSeverityRegistrar(),
                                                                       errorLevel,
                                                                       myModel.getRoot())
                                     : myModel.getRoot();

    if (isSingleInspectionRun) return currentNode;

    String[] groupPath = toolWrapper.getGroupPath();
    if (groupPath.length == 0) {
      LOG.error("groupPath is empty for tool: " + toolWrapper.getShortName() + ", class: " + toolWrapper.getTool().getClass());
    }

    for (String subGroup : groupPath) {
      currentNode = myModel.createGroupNode(subGroup, currentNode);
    }

    return currentNode;
  }

  public boolean areDescriptorNodesSelected() {
    final TreePath[] paths = getSelectionPaths();
    if (paths == null) return false;
    for (TreePath path : paths) {
      if (!(path.getLastPathComponent() instanceof ProblemDescriptionNode)) {
        return false;
      }
    }
    return true;
  }

  public int getSelectedProblemCount() {
    int count = 0;
    for (TreePath path : TreePathRoots.collect(getSelectionPaths())) {
      LevelAndCount[] levels = ((InspectionTreeNode)path.getLastPathComponent()).getProblemLevels();
      for (LevelAndCount level : levels) {
        count += level.getCount();
      }
    }
    return count;
  }

  private static boolean isNodeValidAndIncluded(ProblemDescriptionNode node, boolean allowResolved) {
    return node.isValid() && (allowResolved ||
                              (!node.isExcluded() &&
                               !node.isAlreadySuppressedFromView() &&
                               !node.isQuickFixAppliedFromView()));
  }

  public void removeSelectedProblems() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    TreePath[] selected = getSelectionPaths();
    if (selected == null) return;
    if (!getContext().getUIOptions().FILTER_RESOLVED_ITEMS) {
      for (TreePath path : selected) {
        InspectionTreeNode node = (InspectionTreeNode)path.getLastPathComponent();
        myModel.traverse(node).forEach(InspectionTreeNode::dropProblemCountCaches);
      }
      return;
    }
    Set<InspectionTreeNode> processedNodes = new THashSet<>();
    List<InspectionTreeNode> toRemove = new ArrayList<>();
    List<TreePath> pathsToSelect = new ArrayList<>();
    for (TreePath path : selected) {
      Object[] nodePath = path.getPath();

      // ignore root
      for (int i = 1; i < nodePath.length; i++) {
        InspectionTreeNode node = (InspectionTreeNode) nodePath[i];
        if (!processedNodes.add(node)) continue;

        if (shouldDelete(node)) {
          toRemove.add(node);
          TreePath toSelect = getParentPath(path, nodePath.length - i);
          if (toSelect != null) {
            pathsToSelect.add(toSelect);
          }
          break;
        }
      }
    }

    if (toRemove.isEmpty()) return;
    Set<InspectionTreeNode> parents = new THashSet<>();
    for (InspectionTreeNode node : toRemove) {
      InspectionTreeNode parent = node.getParent();
      if (parent != null) {
        myModel.remove(node);
        parents.add(parent);
      }
    }

    for (InspectionTreeNode parent : parents) {
      parent.dropProblemCountCaches();
    }
    TreePath commonPath = TreePathUtil.findCommonAncestor(pathsToSelect);
    if (commonPath != null) TreeUtil.selectPath(this, commonPath);

    revalidate();
    repaint();
  }

  private static TreePath getParentPath(TreePath path, int ord) {
    TreePath parent = path;
    for (int j = 0; j < ord; j++) {
      parent = parent.getParentPath();
    }
    return parent;
  }

  private boolean shouldDelete(InspectionTreeNode node) {
    if (node instanceof RefElementNode) {
      RefElementNode refElementNode = (RefElementNode)node;
      InspectionToolPresentation presentation = refElementNode.getPresentation();
      RefEntity element = refElementNode.getElement();
      if (element == null ||
          presentation.isProblemResolved(element) ||
          presentation.isExcluded(element) ||
          presentation.isSuppressed(element)) {
        return true;
      }
      List<? extends InspectionTreeNode> children = node.getChildren();
      return !children.isEmpty() && children.stream().allMatch(this::shouldDelete);
    }
    else if (node instanceof ProblemDescriptionNode) {
      ProblemDescriptionNode problemDescriptionNode = (ProblemDescriptionNode)node;
      CommonProblemDescriptor descriptor = problemDescriptionNode.getDescriptor();
      InspectionToolPresentation presentation = problemDescriptionNode.getPresentation();
      if (descriptor == null || presentation.isExcluded(descriptor) || presentation.isProblemResolved(descriptor)) {
        return true;
      }
    }
    else if (node instanceof InspectionGroupNode || node instanceof InspectionSeverityGroupNode || node instanceof InspectionModuleNode || node instanceof InspectionPackageNode) {
      return node.getChildren().stream().allMatch(this::shouldDelete);
    }
    else if (node instanceof InspectionNode) {
      InspectionToolResultExporter presentation = myView.getGlobalInspectionContext().getPresentation(((InspectionNode)node).getToolWrapper());
      SynchronizedBidiMultiMap<RefEntity, CommonProblemDescriptor> problemElements = presentation.getProblemElements();
      if (problemElements.isEmpty()) {
        return true;
      }
      return problemElements.keys().stream().allMatch(entity -> presentation.isExcluded(entity));
    }
    return false;
  }

  @NotNull
  public GlobalInspectionContextImpl getContext() {
    return myView.getGlobalInspectionContext();
  }

  private static String @NotNull [] getGroupPath(@NotNull InspectionGroupNode node) {
    List<String> path = new ArrayList<>(2);
    while (true) {
      InspectionTreeNode parent = node.getParent();
      if (!(parent instanceof InspectionGroupNode)) break;
      node = (InspectionGroupNode)parent;
      path.add(node.getSubGroup());
    }
    return ArrayUtilRt.toStringArray(path);
  }

  @Nullable
  private static Object getVirtualFileOrEntity(@Nullable RefEntity entity) {
    if (entity instanceof RefElement) {
      SmartPsiElementPointer pointer = ((RefElement)entity).getPointer();
      if (pointer != null) {
        VirtualFile file = pointer.getVirtualFile();
        if (file != null) {
          return file;
        }
      }
    }
    return entity;
  }

  private class MyOccurrenceNavigator implements OccurenceNavigator {
    @Override
    public boolean hasNextOccurence() {
      return getNextNode(true) != null;
    }

    @Override
    public boolean hasPreviousOccurence() {
      return getNextNode(false) != null;
    }

    @Override
    public OccurenceInfo goNextOccurence() {
      InspectionTreeNode node = getNextNode(true);
      if (node == null) return null;
      selectNode(node);
      return new OccurenceInfo(createDescriptorForNode(node), -1, -1);
    }

    @Override
    public OccurenceInfo goPreviousOccurence() {
      InspectionTreeNode node = getNextNode(false);
      selectNode(node);
      return node == null ? null : new OccurenceInfo(createDescriptorForNode(node), -1, -1);
    }

    @NotNull
    @Override
    public String getNextOccurenceActionName() {
      return InspectionsBundle.message("inspection.action.go.next");
    }

    @NotNull
    @Override
    public String getPreviousOccurenceActionName() {
      return InspectionsBundle.message("inspection.action.go.prev");
    }

    private InspectionTreeNode getNextNode(boolean next) {
      InspectionTreeNode node = getSelectedNode();
      if (node == null) {
        return null;
      }
      return myModel.traverseFrom(node, next).filter(n -> n != node).filter(n -> isOccurrenceNode(n)).first();
    }

    private InspectionTreeNode getSelectedNode() {
      TreePath path = getSelectionPath();
      if (path == null) return null;
      return (InspectionTreeNode)path.getLastPathComponent();
    }

    private boolean isOccurrenceNode(@NotNull InspectionTreeNode node) {
      if (node.isExcluded()) {
        return false;
      }
      if (node instanceof RefElementNode) {
        final RefElementNode refNode = (RefElementNode)node;
        if (hasDescriptorUnder(refNode)) return false;
        final RefEntity element = refNode.getElement();
        return element != null && element.isValid();
      }
      return node instanceof ProblemDescriptionNode;
    }

    @Nullable
    private Navigatable createDescriptorForNode(@NotNull InspectionTreeNode node) {
      if (node.isExcluded()) {
        return null;
      }
      if (node instanceof RefElementNode) {
        final RefElementNode refNode = (RefElementNode)node;
        if (hasDescriptorUnder(refNode)) return null;
        final RefEntity element = refNode.getElement();
        if (element == null || !element.isValid()) return null;
        if (element instanceof RefElement) {
          return getOpenFileDescriptor((RefElement)element);
        }
      }
      else if (node instanceof ProblemDescriptionNode) {
        ProblemDescriptionNode problemNode = (ProblemDescriptionNode)node;
        boolean isValid = problemNode.isValid() && (!problemNode.isQuickFixAppliedFromView() ||
                                                    problemNode.calculateIsValid());
        return isValid
               ? navigate(problemNode.getDescriptor())
               : InspectionResultsViewUtil.getNavigatableForInvalidNode(problemNode);
      }
      return null;
    }


    @Nullable
    private Navigatable navigate(final CommonProblemDescriptor descriptor) {
      return InspectionResultsView.getSelectedNavigatable(descriptor);
    }

    @Nullable
    private Navigatable getOpenFileDescriptor(final RefElement refElement) {
      PsiElement psiElement = refElement.getPsiElement();
      if (psiElement == null) return null;
      final PsiFile containingFile = psiElement.getContainingFile();
      if (containingFile == null) return null;
      VirtualFile file = containingFile.getVirtualFile();
      if (file == null) return null;
      return PsiNavigationSupport.getInstance().createNavigatable(refElement.getRefManager().getProject(), file,
                                                                  psiElement.getTextOffset());
    }

    private boolean hasDescriptorUnder(RefElementNode node) {
      InspectionTreeNode current = node;
      while (current != null && current.getChildCount() != 0) {
        current = current.getChildAt(0);
        if (current instanceof ProblemDescriptionNode) return true;
      }
      return false;
    }
  }

  private TreePath getPathFor(InspectionTreeNode node) {
    TreePath result = TreePathUtil.pathToTreeNode(node);

    Stack<TreePath> s = new Stack<>();
    TreePath current = result;
    while (current != null) {
      s.add(current);
      current = current.getParentPath();
    }
    while (!s.isEmpty()) {
      TreePath p = s.pop();
      expandPath(p);
    }
    return result;
  }
}
