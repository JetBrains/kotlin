// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.externalSystem.view.ExternalProjectsViewState;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.xmlb.annotations.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class ExternalProjectsState {
  private final Map<String, State> myExternalSystemsState = FactoryMap.create(key -> new State());

  @Property(surroundWithTag = false)
  @XMap(keyAttributeName = "id", entryTagName = "system")
  public Map<String, State> getExternalSystemsState() {
    return myExternalSystemsState;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setExternalSystemsState(Map<String, State> externalSystemsState) {
  }

  @Attribute
  public boolean storeExternally = false;

  @Tag("state")
  public static class State {
    private ExternalProjectsViewState projectsViewState = new ExternalProjectsViewState();

    private final Map<String, TaskActivationState> myExternalSystemsTaskActivation = FactoryMap.createMap(key-> new TaskActivationState(), LinkedHashMap::new);

    @Property(surroundWithTag = false)
    @MapAnnotation(keyAttributeName = "path", entryTagName = "task",
      surroundWithTag = false, surroundValueWithTag = false, surroundKeyWithTag = false, sortBeforeSave = false)
    public Map<String, TaskActivationState> getExternalSystemsTaskActivation() {
      return myExternalSystemsTaskActivation;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setExternalSystemsTaskActivation(Map<String, TaskActivationState> externalSystemsTaskActivation) {
    }

    @Property(surroundWithTag = false)
    public ExternalProjectsViewState getProjectsViewState() {
      return projectsViewState;
    }

    public void setProjectsViewState(ExternalProjectsViewState projectsViewState) {
      this.projectsViewState = projectsViewState;
    }
  }
}
