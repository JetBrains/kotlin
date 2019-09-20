/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.framework.detection.impl.exclude.old;

import com.intellij.framework.detection.impl.exclude.ExcludedFileState;
import com.intellij.framework.detection.impl.exclude.ExcludesConfigurationState;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
@State(name = "FacetAutodetectingManager")
public class OldFacetDetectionExcludesConfiguration implements PersistentStateComponent<DisabledAutodetectionInfo> {
  public static final String COMPONENT_NAME = "FacetAutodetectingManager";

  private DisabledAutodetectionInfo myDisabledAutodetectionInfo;
  private final Project myProject;

  public static OldFacetDetectionExcludesConfiguration getInstance(Project project) {
    return ServiceManager.getService(project, OldFacetDetectionExcludesConfiguration.class);
  }

  public OldFacetDetectionExcludesConfiguration(Project project) {
    myProject = project;
  }

  @Override
  public DisabledAutodetectionInfo getState() {
    return myDisabledAutodetectionInfo;
  }

  @Override
  public void loadState(@NotNull DisabledAutodetectionInfo state) {
    myDisabledAutodetectionInfo = state;
  }

  public void unsetState() {
    myDisabledAutodetectionInfo = null;
  }

  @Nullable
  public ExcludesConfigurationState convert() {
    if (myDisabledAutodetectionInfo == null || myDisabledAutodetectionInfo.getElements().isEmpty()) {
      return null;
    }

    final ExcludesConfigurationState state = new ExcludesConfigurationState();
    for (DisabledAutodetectionByTypeElement element : myDisabledAutodetectionInfo.getElements()) {
      final String frameworkId = element.getFacetTypeId();
      final List<DisabledAutodetectionInModuleElement> moduleElements = element.getModuleElements();
      if (moduleElements.isEmpty()) {
        state.getFrameworkTypes().add(frameworkId);
        continue;
      }
      Set<String> excludedUrls = new LinkedHashSet<>();
      for (DisabledAutodetectionInModuleElement moduleElement : moduleElements) {
        if (moduleElement.isDisableInWholeModule()) {
          final Module module = ModuleManager.getInstance(myProject).findModuleByName(moduleElement.getModuleName());
          if (module != null) {
            Collections.addAll(excludedUrls, ModuleRootManager.getInstance(module).getContentRootUrls());
          }
        }
        else {
          excludedUrls.addAll(moduleElement.getFiles());
          excludedUrls.addAll(moduleElement.getDirectories());
        }
      }
      for (String url : excludedUrls) {
        state.getFiles().add(new ExcludedFileState(url, frameworkId));
      }
    }
    return state;
  }
}
