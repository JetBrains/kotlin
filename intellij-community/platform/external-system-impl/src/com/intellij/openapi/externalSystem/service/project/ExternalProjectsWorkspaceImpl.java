// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.project.ProjectCoordinate;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Property;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
@State(name = "externalSubstitutions", storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
public class ExternalProjectsWorkspaceImpl implements PersistentStateComponent<ExternalProjectsWorkspaceImpl.State> {

  static final ExtensionPointName<ExternalProjectsWorkspaceImpl.Contributor> EP_NAME =
    ExtensionPointName.create("com.intellij.externalSystemWorkspaceContributor");

  @ApiStatus.Experimental
  public interface Contributor {
    @Nullable
    ProjectCoordinate findProjectId(Module module, IdeModifiableModelsProvider modelsProvider);
  }

  static class State {
    @Property(surroundWithTag = false)
    @MapAnnotation(surroundWithTag = false, surroundValueWithTag = false, surroundKeyWithTag = false,
      keyAttributeName = "name", entryTagName = "module")
    public Map<String, Set<String>> substitutions;
    @Property(surroundWithTag = false)
    @MapAnnotation(surroundWithTag = false, surroundValueWithTag = false, surroundKeyWithTag = false,
      keyAttributeName = "module", valueAttributeName = "lib")
    public Map<String, String> names;
  }

  private State myState = new State();

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState = state == null ? new State() : state;
  }

  public static boolean isDependencySubstitutionEnabled() {
    return Registry.is("external.system.substitute.library.dependencies");
  }

  public ModifiableWorkspace createModifiableWorkspace(AbstractIdeModifiableModelsProvider modelsProvider) {
    return new ModifiableWorkspace(myState, modelsProvider);
  }
}
