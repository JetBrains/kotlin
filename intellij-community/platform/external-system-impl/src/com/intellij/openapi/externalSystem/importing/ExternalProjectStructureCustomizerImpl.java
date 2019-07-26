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
package com.intellij.openapi.externalSystem.importing;

import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.AbstractNamedData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.util.Couple;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public class ExternalProjectStructureCustomizerImpl extends ExternalProjectStructureCustomizer {
  private final Set<? extends Key<? extends AbstractNamedData>> myKeys = ContainerUtil.set(ProjectKeys.PROJECT, ProjectKeys.MODULE);

  @NotNull
  @Override
  public Set<? extends Key<?>> getIgnorableDataKeys() {
    return myKeys;
  }

  @NotNull
  @Override
  public Set<? extends Key<?>> getPublicDataKeys() {
    return myKeys;
  }

  @Nullable
  @Override
  public Icon suggestIcon(@NotNull DataNode node, @NotNull ExternalSystemUiAware uiAware) {
    if(ProjectKeys.PROJECT.equals(node.getKey())) {
      return uiAware.getProjectIcon();
    } else if(ProjectKeys.MODULE.equals(node.getKey())) {
      return uiAware.getProjectIcon();
    }
    return null;
  }

  @NotNull
  @Override
  public Couple<String> getRepresentationName(@NotNull DataNode node) {
    if(ProjectKeys.PROJECT.equals(node.getKey())) {
      ProjectData projectData = (ProjectData)node.getData();
      return Couple.of("Project: " + projectData.getExternalName(), projectData.getDescription());
    } else if(ProjectKeys.MODULE.equals(node.getKey())) {
      ModuleData moduleData = (ModuleData)node.getData();
      return Couple.of(moduleData.getId(), moduleData.getDescription());
    }
    return super.getRepresentationName(node);
  }
}
