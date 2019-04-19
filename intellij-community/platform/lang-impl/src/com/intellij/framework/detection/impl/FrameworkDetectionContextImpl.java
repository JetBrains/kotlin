/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.framework.detection.impl;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.FacetType;
import com.intellij.facet.impl.DefaultFacetsProvider;
import com.intellij.framework.detection.DetectedFrameworkDescription;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author nik
 */
public class FrameworkDetectionContextImpl extends FrameworkDetectionContextBase {
  private final Project myProject;

  public FrameworkDetectionContextImpl(@NotNull Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public Project getProject() {
    return myProject;
  }

  @NotNull
  @Override
  public <F extends Facet, C extends FacetConfiguration> List<? extends DetectedFrameworkDescription> createDetectedFacetDescriptions(@NotNull FacetBasedFrameworkDetector<F, C> detector,
                                                                                                                                      @NotNull Collection<VirtualFile> files) {
    MultiMap<Module, VirtualFile> filesByModule = MultiMap.createSet();
    for (VirtualFile file : files) {
      final Module module = ModuleUtilCore.findModuleForFile(file, myProject);
      if (module != null) {
        filesByModule.putValue(module, file);
      }
    }
    final List<DetectedFrameworkDescription> result = new ArrayList<>();
    final FacetType<F,C> facetType = detector.getFacetType();
    final FacetsProvider provider = DefaultFacetsProvider.INSTANCE;
    for (Module module : filesByModule.keySet()) {
      final Collection<F> facets = provider.getFacetsByType(module, facetType.getId());
      if (!facetType.isSuitableModuleType(ModuleType.get(module)) || facetType.isOnlyOneFacetAllowed() && !facets.isEmpty()) {
        continue;
      }
      List<C> existentConfigurations = new ArrayList<>();
      for (F facet : facets) {
        //noinspection unchecked
        existentConfigurations.add((C)facet.getConfiguration());
      }
      final Collection<VirtualFile> moduleFiles = filesByModule.get(module);
      final List<Pair<C, Collection<VirtualFile>>> pairs = detector.createConfigurations(moduleFiles, existentConfigurations);
      for (Pair<C, Collection<VirtualFile>> pair : pairs) {
        result.add(new FacetBasedDetectedFrameworkDescriptionImpl<>(module, detector, pair.getFirst(), new HashSet<>(pair.getSecond())));
      }
    }
    return result;
  }

  @Override
  public VirtualFile getBaseDir() {
    return myProject.getBaseDir();
  }
}
