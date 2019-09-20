/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.service.project.view;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.ExternalSystemViewContributor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GradleViewContributor extends ExternalSystemViewContributor {
  private static final Key<?>[] KEYS = new Key[]{
    GradleSourceSetData.KEY,
  };


  @NotNull
  @Override
  public ProjectSystemId getSystemId() {
    return GradleConstants.SYSTEM_ID;
  }

  @NotNull
  @Override
  public List<Key<?>> getKeys() {
    return Arrays.asList(KEYS);
  }

  @NotNull
  @Override
  public List<ExternalSystemNode<?>> createNodes(ExternalProjectsView externalProjectsView, MultiMap<Key<?>, DataNode<?>> dataNodes) {
    final List<ExternalSystemNode<?>> result = new SmartList<>();
    addCustomSourceSetsNodes(externalProjectsView, dataNodes, result);
    return result;
  }

  @Nullable
  @Override
  public String getDisplayName(@NotNull DataNode node) {
    if (ProjectKeys.MODULE.equals(node.getKey())) {
      return ((ModuleData)node.getData()).getId();
    }
    return super.getDisplayName(node);
  }

  private static void addCustomSourceSetsNodes(@NotNull ExternalProjectsView externalProjectsView,
                                               @NotNull MultiMap<Key<?>, DataNode<?>> dataNodes,
                                               @NotNull List<ExternalSystemNode<?>> result) {
    final Collection<DataNode<?>> sourceSetsDataNodes = dataNodes.get(GradleSourceSetData.KEY);
    if (!sourceSetsDataNodes.isEmpty()) {
      final ExternalSystemNode sourceSetsNode = new SourceSetsNode(externalProjectsView);
      for (DataNode<?> dataNode : sourceSetsDataNodes) {
        //noinspection unchecked
        sourceSetsNode.add(new SourceSetNode(externalProjectsView, (DataNode<GradleSourceSetData>)dataNode));
      }
      result.add(sourceSetsNode);
    }
  }

  @Order(ExternalSystemNode.BUILTIN_TASKS_DATA_NODE_ORDER - 1)
  private static class SourceSetsNode extends ExternalSystemNode {
    SourceSetsNode(ExternalProjectsView externalProjectsView) {
      //noinspection unchecked
      super(externalProjectsView, null, null);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      presentation.setIcon(AllIcons.Nodes.ModuleGroup);
    }

    @Override
    public String getName() {
      return "Source Sets";
    }
  }

  private static class SourceSetNode extends ExternalSystemNode<GradleSourceSetData> {

    SourceSetNode(ExternalProjectsView externalProjectsView, DataNode<GradleSourceSetData> dataNode) {
      super(externalProjectsView, null, dataNode);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      presentation.setIcon(AllIcons.Modules.SourceFolder);

      final GradleSourceSetData data = getData();
      if (data != null) {
        setNameAndTooltip(getName(), null);
      }
    }

    @Override
    public String getName() {
      final GradleSourceSetData data = getData();
      return data != null ? StringUtil.substringAfter(data.getExternalName(), ":") : "";
    }
  }
}
