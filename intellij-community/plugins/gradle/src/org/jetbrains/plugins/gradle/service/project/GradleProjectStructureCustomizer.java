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
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.importing.ExternalProjectStructureCustomizer;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.Identifiable;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;

import javax.swing.*;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public class GradleProjectStructureCustomizer extends ExternalProjectStructureCustomizer {

  private final Set<Key<? extends Identifiable>> myKeys = ContainerUtil.newHashSet(GradleSourceSetData.KEY, ProjectKeys.MODULE);

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

  @NotNull
  @Override
  public Set<? extends Key<? extends Identifiable>> getDependencyAwareDataKeys() {
    return myKeys;
  }

  @Nullable
  @Override
  public Icon suggestIcon(@NotNull DataNode node, @NotNull ExternalSystemUiAware uiAware) {
    return null;
  }

  @NotNull
  @Override
  public Couple<String> getRepresentationName(@NotNull DataNode node) {
    if (node.getKey().equals(GradleSourceSetData.KEY)) {
      final GradleSourceSetData data = (GradleSourceSetData)node.getData();
      return Couple.of("Source Set", StringUtil.substringAfter(data.getExternalName(), ":"));
    }
    if (node.getKey().equals(ProjectKeys.MODULE)) {
      ModuleData moduleData = (ModuleData)node.getData();
      return Couple.of(moduleData.getExternalName(), null);
    }
    return super.getRepresentationName(node);
  }
}
