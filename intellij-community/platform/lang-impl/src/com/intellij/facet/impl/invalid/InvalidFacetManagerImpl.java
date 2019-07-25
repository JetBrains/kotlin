// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl.invalid;

import com.intellij.facet.ProjectFacetManager;
import com.intellij.facet.pointers.FacetPointersManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
@State(name = InvalidFacetManagerImpl.COMPONENT_NAME, storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class InvalidFacetManagerImpl extends InvalidFacetManager implements PersistentStateComponent<InvalidFacetManagerImpl.InvalidFacetManagerState> {
  public static final String COMPONENT_NAME = "InvalidFacetManager";
  private InvalidFacetManagerState myState = new InvalidFacetManagerState();
  private final Project myProject;

  public InvalidFacetManagerImpl(Project project) {
    myProject = project;
  }

  @Override
  public boolean isIgnored(@NotNull InvalidFacet facet) {
    return myState.getIgnoredFacets().contains(FacetPointersManager.constructId(facet));
  }

  @Override
  public InvalidFacetManagerState getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull InvalidFacetManagerState state) {
    myState = state;
  }

  @Override
  public void setIgnored(@NotNull InvalidFacet facet, boolean ignored) {
    final String id = FacetPointersManager.constructId(facet);
    if (ignored) {
      myState.getIgnoredFacets().add(id);
    }
    else {
      myState.getIgnoredFacets().remove(id);
    }
  }

  @Override
  public List<InvalidFacet> getInvalidFacets() {
    return ProjectFacetManager.getInstance(myProject).getFacets(InvalidFacetType.TYPE_ID);
  }

  public static class InvalidFacetManagerState {
    private Set<String> myIgnoredFacets = new HashSet<>();

    @XCollection(propertyElementName = "ignored-facets", elementName = "facet", valueAttributeName = "id")
    public Set<String> getIgnoredFacets() {
      return myIgnoredFacets;
    }

    public void setIgnoredFacets(Set<String> ignoredFacets) {
      myIgnoredFacets = ignoredFacets;
    }
  }
}
