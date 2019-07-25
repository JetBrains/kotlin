// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.task.ui;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskExecutionInfo;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskPojo;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.*;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemTasksTreeModel extends DefaultTreeModel {

  @NotNull private static final Comparator<TreeNode> NODE_COMPARATOR = (t1, t2) -> {
    Object e1 = ((ExternalSystemNode<?>)t1).getDescriptor().getElement();
    Object e2 = ((ExternalSystemNode<?>)t2).getDescriptor().getElement();
    if (e1 instanceof ExternalProjectPojo) {
      if (e2 instanceof ExternalTaskExecutionInfo) {
        return 1;
      }
      else {
        return ((ExternalProjectPojo)e1).getName().compareTo(((ExternalProjectPojo)e2).getName());
      }
    }
    else {
      if (e2 instanceof ExternalProjectPojo) {
        return -1;
      }
      else {
        return getTaskName((ExternalTaskExecutionInfo)e1).compareTo(getTaskName((ExternalTaskExecutionInfo)e2));
      }
    }
  };

  @NotNull private final ExternalSystemUiAware myUiAware;
  @NotNull private final ProjectSystemId myExternalSystemId;

  public ExternalSystemTasksTreeModel(@NotNull ProjectSystemId externalSystemId) {
    super(new ExternalSystemNode<>(new ExternalSystemNodeDescriptor<>("", "", "", null)));
    myExternalSystemId = externalSystemId;
    myUiAware = ExternalSystemUiUtil.getUiAware(externalSystemId);
  }

  private static String getTaskName(@NotNull ExternalTaskExecutionInfo taskInfo) {
    return taskInfo.getSettings().getTaskNames().get(0);
  }

  /**
   * Ensures that current model has a top-level node which corresponds to the given external project info holder
   *
   * @param project target external project info holder
   */
  @SuppressWarnings("unchecked")
  @NotNull
  public ExternalSystemNode<ExternalProjectPojo> ensureProjectNodeExists(@NotNull ExternalProjectPojo project) {
    ExternalSystemNode<?> root = getRoot();

    // Remove outdated projects.
    for (int i = root.getChildCount() - 1; i >= 0; i--) {
      ExternalSystemNode<?> child = root.getChildAt(i);
      ExternalSystemNodeDescriptor<?> descriptor = child.getDescriptor();
      Object element = descriptor.getElement();
      if (element instanceof ExternalProjectPojo) {
        ExternalProjectPojo pojo = (ExternalProjectPojo)element;
        if (pojo.getPath().equals(project.getPath())) {
          if (!pojo.getName().equals(project.getName())) {
            pojo.setName(project.getName());
            descriptor.setName(project.getName());
            nodeChanged(child);
          }
          return (ExternalSystemNode<ExternalProjectPojo>)child;
        }
      }
    }
    ExternalProjectPojo element = new ExternalProjectPojo(project.getName(), project.getPath());
    ExternalSystemNodeDescriptor<ExternalProjectPojo> descriptor = descriptor(element, myUiAware.getProjectIcon());
    ExternalSystemNode<ExternalProjectPojo> result = new ExternalSystemNode<>(descriptor);
    insertNodeInto(result, root);
    return result;
  }

  /**
   * Asks current model to remove all nodes which have given data as a {@link ExternalSystemNodeDescriptor#getElement() payload}.
   *
   * @param payload target payload
   */
  public void pruneNodes(@NotNull Object payload) {
    Deque<ExternalSystemNode<?>> toProcess = new ArrayDeque<>();
    toProcess.addFirst(getRoot());
    while (!toProcess.isEmpty()) {
      ExternalSystemNode<?> node = toProcess.removeLast();
      if (payload.equals(node.getDescriptor().getElement())) {
        removeNodeFromParent(node);
      }
      else {
        for (int i = 0; i < node.getChildCount(); i++) {
          toProcess.addFirst(node.getChildAt(i));
        }
      }
    }
  }

  public void ensureSubProjectsStructure(@NotNull ExternalProjectPojo topLevelProject,
                                         @NotNull Collection<? extends ExternalProjectPojo> subProjects) {
    ExternalSystemNode<ExternalProjectPojo> topLevelProjectNode = ensureProjectNodeExists(topLevelProject);
    Map<String/*config path*/, ExternalProjectPojo> toAdd = new HashMap<>();
    for (ExternalProjectPojo subProject : subProjects) {
      toAdd.put(subProject.getPath(), subProject);
    }
    toAdd.remove(topLevelProject.getPath());

    final TObjectIntHashMap<Object> taskWeights = new TObjectIntHashMap<>();
    for (int i = 0; i < topLevelProjectNode.getChildCount(); i++) {
      ExternalSystemNode<?> child = topLevelProjectNode.getChildAt(i);
      Object childElement = child.getDescriptor().getElement();
      if (childElement instanceof ExternalTaskExecutionInfo) {
        taskWeights.put(childElement, subProjects.size() + i);
        continue;
      }
      if (toAdd.remove(((ExternalProjectPojo)childElement).getPath()) == null) {
        removeNodeFromParent(child);
        //noinspection AssignmentToForLoopParameter
        i--;
      }
    }
    if (!toAdd.isEmpty()) {
      for (Map.Entry<String, ExternalProjectPojo> entry : toAdd.entrySet()) {
        ExternalProjectPojo
          element = new ExternalProjectPojo(entry.getValue().getName(), entry.getValue().getPath());
        insertNodeInto(new ExternalSystemNode<>(descriptor(element, myUiAware.getProjectIcon())),
                       topLevelProjectNode);
      }
    }
  }

  public void ensureTasks(@NotNull String externalProjectConfigPath, @NotNull Collection<? extends ExternalTaskPojo> tasks) {
    if (tasks.isEmpty()) {
      return;
    }
    ExternalSystemNode<ExternalProjectPojo> moduleNode = findProjectNode(externalProjectConfigPath);
    if (moduleNode == null) {
//      LOG.warn(String.format(
//        "Can't proceed tasks for module which external config path is '%s'. Reason: no such module node is found. Tasks: %s",
//        externalProjectConfigPath, tasks
//      ));
      return;
    }
    Set<ExternalTaskExecutionInfo> toAdd = new HashSet<>();
    for (ExternalTaskPojo task : tasks) {
      toAdd.add(buildTaskInfo(task));
    }
    for (int i = 0; i < moduleNode.getChildCount(); i++) {
      ExternalSystemNode<?> childNode = moduleNode.getChildAt(i);
      Object element = childNode.getDescriptor().getElement();
      if (element instanceof ExternalTaskExecutionInfo) {
        if (!toAdd.remove(element)) {
          removeNodeFromParent(childNode);
          //noinspection AssignmentToForLoopParameter
          i--;
        }
      }
    }

    if (!toAdd.isEmpty()) {
      for (ExternalTaskExecutionInfo taskInfo : toAdd) {
        insertNodeInto(
          new ExternalSystemNode<>(descriptor(taskInfo, taskInfo.getDescription(), myUiAware.getTaskIcon())),
          moduleNode);
      }
    }
  }

  @NotNull
  private ExternalTaskExecutionInfo buildTaskInfo(@NotNull ExternalTaskPojo task) {
    ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
    settings.setExternalProjectPath(task.getLinkedExternalProjectPath());
    settings.setTaskNames(Collections.singletonList(task.getName()));
    settings.setTaskDescriptions(Collections.singletonList(task.getDescription()));
    settings.setExternalSystemIdString(myExternalSystemId.toString());
    return new ExternalTaskExecutionInfo(settings, DefaultRunExecutor.EXECUTOR_ID);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private ExternalSystemNode<ExternalProjectPojo> findProjectNode(@NotNull String configPath) {
    for (int i = getRoot().getChildCount() - 1; i >= 0; i--) {
      ExternalSystemNode<?> child = getRoot().getChildAt(i);
      Object childElement = child.getDescriptor().getElement();
      if (childElement instanceof ExternalProjectPojo && ((ExternalProjectPojo)childElement).getPath().equals(configPath)) {
        return (ExternalSystemNode<ExternalProjectPojo>)child;
      }
      for (int j = child.getChildCount() - 1; j >= 0; j--) {
        ExternalSystemNode<?> grandChild = child.getChildAt(j);
        Object grandChildElement = grandChild.getDescriptor().getElement();
        if (grandChildElement instanceof ExternalProjectPojo
            && ((ExternalProjectPojo)grandChildElement).getPath().equals(configPath)) {
          return (ExternalSystemNode<ExternalProjectPojo>)grandChild;
        }
      }
    }
    return null;
  }

  @NotNull
  private static <T> ExternalSystemNodeDescriptor<T> descriptor(@NotNull T element, @Nullable Icon icon) {
    return descriptor(element, "", icon);
  }

  @NotNull
  private static <T> ExternalSystemNodeDescriptor<T> descriptor(@NotNull T element, @NotNull String description, @Nullable Icon icon) {
    return new ExternalSystemNodeDescriptor<>(element, element.toString(), description, icon);
  }

  @Override
  @NotNull
  public ExternalSystemNode<?> getRoot() {
    return (ExternalSystemNode<?>)super.getRoot();
  }

  public void insertNodeInto(MutableTreeNode child, MutableTreeNode parent) {
    int index = findIndexFor(child, parent);
    super.insertNodeInto(child, parent, index);
  }

  @Override
  public void insertNodeInto(MutableTreeNode child, MutableTreeNode parent, int i) {
    insertNodeInto(child, parent);
  }

  private static int findIndexFor(MutableTreeNode child, MutableTreeNode parent) {
    int childCount = parent.getChildCount();
    if (childCount == 0) {
      return 0;
    }
    if (childCount == 1) {
      return NODE_COMPARATOR.compare(child, parent.getChildAt(0)) <= 0 ? 0 : 1;
    }
    return findIndexFor(child, parent, 0, childCount - 1);
  }

  private static int findIndexFor(MutableTreeNode child, MutableTreeNode parent, int i1, int i2) {
    if (i1 == i2) {
      return NODE_COMPARATOR.compare(child, parent.getChildAt(i1)) <= 0 ? i1 : i1 + 1;
    }
    int half = (i1 + i2) / 2;
    if (NODE_COMPARATOR.compare(child, parent.getChildAt(half)) <= 0) {
      return findIndexFor(child, parent, i1, half);
    }
    return findIndexFor(child, parent, half + 1, i2);
  }
}
