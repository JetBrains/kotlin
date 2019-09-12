// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project;

import com.intellij.openapi.externalSystem.model.project.ProjectCoordinate;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashMap;
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

  private final Map<ProjectCoordinate, String> myModuleMappingById = new THashMap<>(new TObjectHashingStrategy<ProjectCoordinate>() {
    @Override
    public int computeHashCode(ProjectCoordinate object) {
      String groupId = object.getGroupId();
      String artifactId = object.getArtifactId();
      String version = object.getVersion();
      int result1 = (groupId != null ? groupId.hashCode() : 0);
      result1 = 31 * result1 + (artifactId != null ? artifactId.hashCode() : 0);
      result1 = 31 * result1 + (version != null ? version.hashCode() : 0);
      return result1;
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
  private final Map<String /* module name */, String /* library name */> myNamesMap = new HashMap<>();


  public ModifiableWorkspace(ExternalProjectsWorkspaceImpl.State state,
                             AbstractIdeModifiableModelsProvider modelsProvider) {
    myModelsProvider = modelsProvider;
    Set<String> existingModules = new HashSet<>();
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
    Set<String> existingModules = new HashSet<>();
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
    return myNamesMap.containsValue(libraryName);
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
