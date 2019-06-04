// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.view;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.treeStructure.*;
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class ExternalProjectsStructure extends SimpleTreeStructure implements Disposable  {
  private final Project myProject;
  private final SimpleTree myTree;
  private ExternalProjectsView myExternalProjectsView;
  private SimpleTreeBuilder myTreeBuilder;
  private RootNode myRoot;

  private final Map<String, ExternalSystemNode> myNodeMapping = new THashMap<>();

  public ExternalProjectsStructure(Project project, SimpleTree tree) {
    myProject = project;
    myTree = tree;
    configureTree(tree);
  }

  public void init(ExternalProjectsView externalProjectsView) {
    myExternalProjectsView = externalProjectsView;
    myRoot = new RootNode();

    myTreeBuilder = new SimpleTreeBuilder(myTree, (DefaultTreeModel)myTree.getModel(), this, null) {
      // unique class to simplify search through the logs
    };
    Disposer.register(myProject, myTreeBuilder);
    myTreeBuilder.initRoot();
    myTreeBuilder.expand(myRoot, null);
  }

  public Project getProject() {
    return myProject;
  }

  public void updateFrom(SimpleNode node) {
    if (node != null) {
      myTreeBuilder.addSubtreeToUpdateByElement(node);
    }
  }

  public void updateUpTo(SimpleNode node) {
    SimpleNode each = node;
    while (each != null) {
      updateFrom(each);
      each = each.getParent();
    }
  }

  @NotNull
  @Override
  public Object getRootElement() {
    return myRoot;
  }

  private static void configureTree(final SimpleTree tree) {
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
  }

  public void accept(@NotNull SimpleNodeVisitor visitor) {
    if (myTreeBuilder.getTree() instanceof SimpleTree) {
      ((SimpleTree)myTreeBuilder.getTree()).accept(myTreeBuilder, visitor);
    }
  }

  public void select(SimpleNode node) {
    myTreeBuilder.select(node, null);
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
        myTreeBuilder.expand(projectNode, null);
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

  public <T extends ExternalSystemNode> void updateNodes(@NotNull Class<? extends T> nodeClass) {
    for (T node : getNodes(nodeClass)) {
      updateFrom(node);
    }
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
    final List<T> filtered = new ArrayList<>();
    for (SimpleNode node : getSelectedNodes(tree)) {
      if ((nodeClass != null) && (!nodeClass.isInstance(node))) {
        filtered.clear();
        break;
      }
      //noinspection unchecked
      filtered.add((T)node);
    }
    return filtered;
  }

  private static List<SimpleNode> getSelectedNodes(SimpleTree tree) {
    List<SimpleNode> nodes = new ArrayList<>();
    TreePath[] treePaths = tree.getSelectionPaths();
    if (treePaths != null) {
      for (TreePath treePath : treePaths) {
        nodes.add(tree.getNodeFor(treePath));
      }
    }
    return nodes;
  }
}
