/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.project;

import com.intellij.openapi.externalSystem.model.project.ProjectCoordinate;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public class ModifiableWorkspace {

  private final Map<ProjectCoordinate, String> myModuleMappingById = ContainerUtil.newTroveMap(
    new TObjectHashingStrategy<ProjectCoordinate>() {
      @Override
      public int computeHashCode(ProjectCoordinate object) {
        String groupId = object.getGroupId();
        String artifactId = object.getArtifactId();
        String version = object.getVersion();
        int result = (groupId != null ? groupId.hashCode() : 0);
        result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
      }

      @Override
      public boolean equals(ProjectCoordinate o1, ProjectCoordinate o2) {
        if (o1.getGroupId() != null ? !o1.getGroupId().equals(o2.getGroupId()) : o2.getGroupId() != null) return false;
        if (o1.getArtifactId() != null ? !o1.getArtifactId().equals(o2.getArtifactId()) : o2.getArtifactId() != null) return false;
        if (o1.getVersion() != null ? !o1.getVersion().equals(o2.getVersion()) : o2.getVersion() != null) return false;
        return true;
      }
    });
  private final AbstractIdeModifiableModelsProvider myModelsProvider;
  private final ExternalProjectsWorkspaceImpl.State myState;
  private final MultiMap<String/* module owner */, String /* substitution modules */> mySubstitutions = MultiMap.createSet();
  private final Map<String /* module name */, String /* library name */> myNamesMap = ContainerUtil.newHashMap();


  public ModifiableWorkspace(ExternalProjectsWorkspaceImpl.State state,
                             AbstractIdeModifiableModelsProvider modelsProvider) {
    myModelsProvider = modelsProvider;
    Set<String> existingModules = ContainerUtil.newHashSet();
    for (Module module : modelsProvider.getModules()) {
      register(module, modelsProvider);
      existingModules.add(module.getName());
    }
    myState = state;
    if (myState.names != null) {
      for (Map.Entry<String, String> entry : myState.names.entrySet()) {
        if (existingModules.contains(entry.getKey())) {
          myNamesMap.put(entry.getKey(), entry.getValue());
        }
      }
    }

    if (myState.substitutions != null) {
      for (Map.Entry<String, Set<String>> entry : myState.substitutions.entrySet()) {
        if (existingModules.contains(entry.getKey())) {
          mySubstitutions.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public void commit() {
    Set<String> existingModules = ContainerUtil.newHashSet();
    Arrays.stream(myModelsProvider.getModules()).map(Module::getName).forEach(existingModules::add);

    myState.names = new HashMap<>();
    myNamesMap.forEach((module, lib) -> {
      if (existingModules.contains(module)) {
        myState.names.put(module, lib);
      }
    });

    myState.substitutions = new HashMap<>();
    for (Map.Entry<String, Collection<String>> entry : mySubstitutions.entrySet()) {
      if (!existingModules.contains(entry.getKey())) continue;
      Collection<String> value = entry.getValue();
      if (value != null && !value.isEmpty()) {
        myState.substitutions.put(entry.getKey(), new TreeSet<>(value));
      }
    }
  }

  public void addSubstitution(String ownerModuleName,
                              String moduleName,
                              String libraryName,
                              DependencyScope scope) {
    myNamesMap.put(moduleName, libraryName);
    mySubstitutions.putValue(ownerModuleName, moduleName + '_' + scope.getDisplayName());
  }

  public void removeSubstitution(String ownerModuleName,
                                 String moduleName,
                                 String libraryName,
                                 DependencyScope scope) {
    mySubstitutions.remove(ownerModuleName, moduleName + '_' + scope.getDisplayName());
    Collection<? extends String> substitutions = mySubstitutions.values();
    for (DependencyScope dependencyScope : DependencyScope.values()) {
      if (substitutions.contains(moduleName + '_' + dependencyScope.getDisplayName())) {
        return;
      }
    }
    myNamesMap.remove(moduleName, libraryName);
  }

  public boolean isSubstitution(String moduleOwner, String substitutionModule, DependencyScope scope) {
    return mySubstitutions.get(moduleOwner).contains(substitutionModule + '_' + scope.getDisplayName());
  }

  public boolean isSubstituted(String libraryName) {
    return myNamesMap.values().contains(libraryName);
  }

  public String getSubstitutedLibrary(String moduleName) {
    return myNamesMap.get(moduleName);
  }

  @Nullable
  public String findModule(@NotNull ProjectCoordinate id) {
    if (StringUtil.isEmpty(id.getArtifactId())) return null;
    String result = myModuleMappingById.get(id);
    return result == null && id.getVersion() != null
           ? myModuleMappingById.get(new ProjectId(id.getGroupId(), id.getArtifactId(), null))
           : result;
  }

  public void register(@NotNull ProjectCoordinate id, @NotNull Module module) {
    myModuleMappingById.put(id, module.getName());
    myModuleMappingById.put(new ProjectId(id.getGroupId(), id.getArtifactId(), null), module.getName());
  }

  private void register(@NotNull Module module, AbstractIdeModifiableModelsProvider modelsProvider) {
    Arrays.stream(ExternalProjectsWorkspaceImpl.EP_NAME.getExtensions())
      .map(contributor -> contributor.findProjectId(module, modelsProvider))
      .filter(Objects::nonNull)
      .findFirst()
      .ifPresent(id -> register(id, module));
  }
}
