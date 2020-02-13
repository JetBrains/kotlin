// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.view;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreePath;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class ExternalProjectsStructure extends SimpleTreeStructure implements Disposable  {
  private final Project myProject;
  private final Tree myTree;
  private ExternalProjectsView myExternalProjectsView;
  private StructureTreeModel<ExternalProjectsStructure> myTreeModel;
  private RootNode myRoot;

  private final Map<String, ExternalSystemNode> myNodeMapping = new THashMap<>();
  private AsyncTreeModel myAsyncTreeModel;

  public ExternalProjectsStructure(Project project, Tree tree) {
    myProject = project;
    myTree = tree;
    configureTree(tree);
  }

  public void init(ExternalProjectsView externalProjectsView) {
    myExternalProjectsView = externalProjectsView;
    myRoot = new RootNode();
    myTreeModel = new StructureTreeModel<>(this, this);
    myAsyncTreeModel = new AsyncTreeModel(myTreeModel, this);
    myTree.setModel(myAsyncTreeModel);
    TreeUtil.expand(myTree, 1);
  }

  public Project getProject() {
    return myProject;
  }

  public void updateFrom(SimpleNode node) {
    if (node != null) {
      myTreeModel.invalidate(node, true);
    }
  }

  public void updateUpTo(SimpleNode node) {
    SimpleNode each = node;
    while (each != null) {
      myTreeModel.invalidate(each, false);
      each = each.getParent();
    }
  }

  @NotNull
  @Override
  public Object getRootElement() {
    return myRoot;
  }

  public void cleanupCache() {
    myRoot.cleanUpCache();
    myNodeMapping.clear();
    myTreeModel.invalidate();
  }

  private static void configureTree(final Tree tree) {
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
  }

  public void select(@NotNull SimpleNode node) {
    myTreeModel.select(node, myTree, path -> {});
  }

  public void expand(@NotNull SimpleNode node) {
    myTreeModel.expand(node, myTree, path -> {});
  }

  protected Class<? extends ExternalSystemNode>[] getVisibleNodesClasses() {
    return null;
  }

  public void updateProjects(Collection<? extends DataNode<ProjectData>> toImport) {
    List<String> orphanProjects = ContainerUtil.mapNotNull(
      myNodeMapping.entrySet(), entry -> entry.getValue() instanceof ProjectNode ? entry.getKey() : null);
    for (DataNode<ProjectData> each : toImport) {
      final ProjectData projectData = each.getData();
      final String projectPath = projectData.getLinkedExternalProjectPath();
      orphanProjects.remove(projectPath);

      ExternalSystemNode projectNode = findNodeFor(projectPath);

      if (projectNode instanceof ProjectNode) {
        doMergeChildrenChanges(projectNode, each, new ProjectNode(myExternalProjectsView, each));
      }
      else {
        ExternalSystemNode node = myNodeMapping.remove(projectPath);
        if (node != null) {
          SimpleNode parent = node.getParent();
          if (parent instanceof ExternalSystemNode) {
            ((ExternalSystemNode)parent).remove(projectNode);
          }
        }

        projectNode = new ProjectNode(myExternalProjectsView, each);
        myNodeMapping.put(projectPath, projectNode);
      }
      if (toImport.size() == 1) {
        TreeUtil.expand(myTree, 1);
      }
      doUpdateProject((ProjectNode)projectNode);
    }

    //remove orphan projects from view
    for (String orphanProjectPath : orphanProjects) {
      ExternalSystemNode projectNode = myNodeMapping.remove(orphanProjectPath);
      if (projectNode instanceof ProjectNode) {
        SimpleNode parent = projectNode.getParent();
        if (parent instanceof ExternalSystemNode) {
          ((ExternalSystemNode)parent).remove(projectNode);
          updateUpTo(projectNode);
        }
      }
    }
  }

  private void doMergeChildrenChanges(ExternalSystemNode currentNode, DataNode<?> newDataNode, ExternalSystemNode newNode) {
    final ExternalSystemNode[] cached = currentNode.getCached();
    if (cached != null) {

      final List<Object> duplicates = new ArrayList<>();
      final Map<Object, ExternalSystemNode> oldDataMap = new LinkedHashMap<>();
      for (ExternalSystemNode node : cached) {
        Object key = node.getData() != null ? node.getData() : node.getName();
        final Object systemNode = oldDataMap.put(key, node);
        if(systemNode != null) {
          duplicates.add(key);
        }
      }

      Map<Object, ExternalSystemNode> newDataMap = new LinkedHashMap<>();
      Map<Object, ExternalSystemNode> unchangedNewDataMap = new LinkedHashMap<>();
      for (ExternalSystemNode node : newNode.getChildren()) {
        Object key = node.getData() != null ? node.getData() : node.getName();
        if (oldDataMap.remove(key) == null) {
          newDataMap.put(key, node);
        }
        else {
          unchangedNewDataMap.put(key, node);
        }
      }

      for (Object duplicate : duplicates) {
        newDataMap.remove(duplicate);
      }

      currentNode.removeAll(oldDataMap.values());

      for (ExternalSystemNode node : currentNode.getChildren()) {
        Object key = node.getData() != null ? node.getData() : node.getName();
        final ExternalSystemNode unchangedNewNode = unchangedNewDataMap.get(key);
        if (unchangedNewNode != null) {
          doMergeChildrenChanges(node, unchangedNewNode.myDataNode, unchangedNewNode);
        }
      }

      updateFrom(currentNode);
      //noinspection unchecked
      currentNode.mergeWith(newNode);
      currentNode.addAll(newDataMap.values());
    } else {
      //noinspection unchecked
      currentNode.mergeWith(newNode);
    }
  }

  private void doUpdateProject(ProjectNode node) {
    ExternalSystemNode newParentNode = myRoot;
    if (!node.isVisible()) {
      newParentNode.remove(node);
    }
    else {
      node.updateProject();
      reconnectNode(node, newParentNode);
    }
  }

  private static void reconnectNode(ProjectNode node, ExternalSystemNode newParentNode) {
    ExternalSystemNode oldParentNode = node.getGroup();
    if (oldParentNode == null || !oldParentNode.equals(newParentNode)) {
      if (oldParentNode != null) {
        oldParentNode.remove(node);
      }
      newParentNode.add(node);
    }
  }

  private ExternalSystemNode findNodeFor(String projectPath) {
    return myNodeMapping.get(projectPath);
  }


  public <T extends ExternalSystemNode> void updateNodesAsync(@NotNull Collection<Class<? extends T>> nodeClasses) {
    updateNodesAsync(nodeClasses, false);
  }
  public <T extends ExternalSystemNode> void updateNodesAsync(@NotNull Collection<Class<? extends T>> nodeClasses, boolean structure) {
    final List<TreePath> nodes = new LinkedList<>();
    myAsyncTreeModel
      .accept(new CollectingVisitor<>(nodes, nodeClasses), false)
      .onSuccess(p -> {
          invalidatePaths(nodes, structure);
      });
  }

  private void invalidatePaths(@NotNull Collection<TreePath> paths, boolean structure) {
    if (paths.isEmpty()) {
      return;
    }
    paths.forEach(p -> myTreeModel.invalidate(p, structure));
  }

  public <T extends ExternalSystemNode> void visitNodes(@NotNull Class<? extends T> nodeClass, @NotNull Consumer<? super T> consumer) {
    for (T node : getNodes(nodeClass)) {
      consumer.consume(node);
    }
  }

  @Override
  public void dispose() {
    this.myExternalProjectsView = null;
    this.myNodeMapping.clear();
    this.myRoot = null;
  }

  private static class CollectingVisitor<T extends ExternalSystemNode> implements TreeVisitor {
    private final List<TreePath> myCollector;
    private final Collection<Class<? extends T>> myNodeClasses;

    public CollectingVisitor(List<TreePath> nodeCollector, Collection<Class<? extends T>> nodeClasses) {
      myCollector = nodeCollector;
      myNodeClasses = nodeClasses;
    }

    @NotNull
    @Override
    public Action visit(@NotNull TreePath path) {
      Object object = TreeUtil.getLastUserObject(path);
      if (object != null && anyAssignableFrom(object.getClass())) {
        myCollector.add(path);
      }
      return Action.CONTINUE;
    }

    boolean anyAssignableFrom(Class<?> classParam) {
      for (Class<? extends T> aClass : myNodeClasses) {
        if (aClass.isAssignableFrom(classParam)) {
          return true;
        }
      }
      return false;
    }
  }

  public class RootNode<T> extends ExternalSystemNode<T> {
    public RootNode() {
      super(myExternalProjectsView, null, null);
    }

    @Override
    public boolean isVisible() {
      return true;
    }
  }

  public enum ErrorLevel {
    NONE, ERROR
  }

  enum DisplayKind {
    ALWAYS, NEVER, NORMAL
  }

  @NotNull
  public <T extends ExternalSystemNode> List<T> getNodes(@NotNull Class<T> nodeClass) {
    return doGetNodes(nodeClass, myRoot.getChildren(), new SmartList<>());
  }

  @NotNull
  private static <T extends ExternalSystemNode> List<T> doGetNodes(@NotNull Class<T> nodeClass,
                                                                   SimpleNode[] nodes,
                                                                   @NotNull List<T> result) {
    if (nodes == null) return result;

    for (SimpleNode node : nodes) {
      if (nodeClass.isInstance(node)) {
        //noinspection unchecked
        result.add((T)node);
      }
      doGetNodes(nodeClass, node.getChildren(), result);
    }
    return result;
  }

  @NotNull
  public <T extends ExternalSystemNode> List<T> getSelectedNodes(SimpleTree tree, Class<T> nodeClass) {
    return TreeUtil.collectSelectedObjectsOfType(tree, nodeClass);
  }
}
