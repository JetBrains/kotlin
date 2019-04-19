/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.view;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import icons.ExternalSystemIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemNode.BUILTIN_TASKS_DATA_NODE_ORDER)
public class TasksNode extends ExternalSystemNode {

  private final MultiMap<String, TaskNode> myTasksMap = new MultiMap<>();

  @SuppressWarnings("unchecked")
  public TasksNode(ExternalProjectsView externalProjectsView, final Collection<? extends DataNode<?>> dataNodes) {
    super(externalProjectsView, null, null);

    if (dataNodes != null && !dataNodes.isEmpty()) {
      for (DataNode<?> dataNode : dataNodes) {
        if (!(dataNode.getData() instanceof TaskData)) continue;
        String group = ((TaskData)dataNode.getData()).getGroup();
        if (group == null) group = "other";
        myTasksMap.putValue(StringUtil.toLowerCase(group), new TaskNode(externalProjectsView, (DataNode<TaskData>)dataNode));
      }
    }
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    super.update(presentation);
    presentation.setIcon(ExternalSystemIcons.TaskGroup);
  }

  @Override
  public String getName() {
    return "Tasks";
  }

  @Override
  public boolean isVisible() {
    return super.isVisible() && hasChildren();
  }

  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  protected List<? extends ExternalSystemNode> doBuildChildren() {
    final List<ExternalSystemNode<?>> result = ContainerUtil.newArrayList();
    final boolean isGroup = getExternalProjectsView().getGroupTasks();
    if (isGroup) {
      for (Map.Entry<String, Collection<TaskNode>> collectionEntry : myTasksMap.entrySet()) {
        final String group = ObjectUtils.notNull(collectionEntry.getKey(), "other");
        final ExternalSystemNode tasksGroupNode = new ExternalSystemNode(getExternalProjectsView(), null, null) {

          @Override
          protected void update(@NotNull PresentationData presentation) {
            super.update(presentation);
            presentation.setIcon(ExternalSystemIcons.TaskGroup);
          }

          @Override
          public String getName() {
            return group;
          }

          @Override
          public boolean isVisible() {
            return super.isVisible() && hasChildren();
          }

          @Override
          public int compareTo(@NotNull ExternalSystemNode node) {
            return "other".equals(group) ? 1 : super.compareTo(node);
          }
        };
        tasksGroupNode.addAll(collectionEntry.getValue());
        result.add(tasksGroupNode);
      }
    }
    else {
      result.addAll(myTasksMap.values());
    }
    return result;
  }
}
