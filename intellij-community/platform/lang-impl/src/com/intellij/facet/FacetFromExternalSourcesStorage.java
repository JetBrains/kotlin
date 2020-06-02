// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectModelElement;
import com.intellij.openapi.roots.ProjectModelExternalSource;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.facet.FacetManagerState;
import org.jetbrains.jps.model.serialization.facet.JpsFacetSerializer;

/**
 * This class isn't used in the new implementation of project model, which is based on {@link com.intellij.workspaceModel.ide Workspace Model}.
 */
@State(name = "External" + JpsFacetSerializer.FACET_MANAGER_COMPONENT_NAME, externalStorageOnly = true)
public final class FacetFromExternalSourcesStorage implements PersistentStateComponent<FacetManagerState>, ProjectModelElement {
  private FacetManagerState myState = new FacetManagerState();
  private final Module myModule;

  public static FacetFromExternalSourcesStorage getInstance(@NotNull Module module) {
    return module.getService(FacetFromExternalSourcesStorage.class);
  }

  public FacetFromExternalSourcesStorage(@NotNull Module module) {
    myModule = module;
  }

  @Override
  public @NotNull FacetManagerState getState() {
    myState = ((FacetManagerImpl)FacetManager.getInstance(myModule)).saveState(FacetManagerImpl.getImportedFacetPredicate(myModule.getProject()));
    return myState;
  }

  @NotNull
  FacetManagerState getLoadedState() {
    return myState;
  }

  @Override
  public @Nullable ProjectModelExternalSource getExternalSource() {
    //If different facets came from different external sources it actually doesn't matter which source is returned from this method,
    // it's enough to return any non-null value to serialize this component into a separate file.
    return ContainerUtil.getFirstItem(((FacetManagerImpl)FacetManager.getInstance(myModule)).getExternalSources());
  }

  @Override
  public void loadState(@NotNull FacetManagerState state) {
    myState = state;
  }
}
