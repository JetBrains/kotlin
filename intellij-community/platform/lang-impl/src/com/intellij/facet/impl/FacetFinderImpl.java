/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.facet.impl;

import com.intellij.facet.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author nik
 */
public class FacetFinderImpl extends FacetFinder {
  private static final Logger LOG = Logger.getInstance(FacetFinderImpl.class);
  private final Map<FacetTypeId, AllFacetsOfTypeModificationTracker> myAllFacetTrackers = new HashMap<>();
  private final Map<FacetTypeId, CachedValue<Map<VirtualFile, List<Facet>>>> myCachedMaps =
    new HashMap<>();
  private final Project myProject;
  private final CachedValuesManager myCachedValuesManager;
  private final ModuleManager myModuleManager;

  public FacetFinderImpl(Project project) {
    myProject = project;
    myCachedValuesManager = CachedValuesManager.getManager(project);
    myModuleManager = ModuleManager.getInstance(myProject);
  }

  @Override
  public <F extends Facet> ModificationTracker getAllFacetsOfTypeModificationTracker(FacetTypeId<F> type) {
    AllFacetsOfTypeModificationTracker tracker = myAllFacetTrackers.get(type);
    if (tracker == null) {
      tracker = new AllFacetsOfTypeModificationTracker<>(myProject, type);
      Disposer.register(myProject, tracker);
      myAllFacetTrackers.put(type, tracker);
    }
    return tracker;
  }

  private <F extends Facet & FacetRootsProvider> Map<VirtualFile, List<Facet>> getRootToFacetsMap(final FacetTypeId<F> type) {
    CachedValue<Map<VirtualFile, List<Facet>>> cachedValue = myCachedMaps.get(type);
    if (cachedValue == null) {
      cachedValue = myCachedValuesManager.createCachedValue(() -> {
        Map<VirtualFile, List<Facet>> map = computeRootToFacetsMap(type);
        return CachedValueProvider.Result.create(map, getAllFacetsOfTypeModificationTracker(type));
      }, false);
      myCachedMaps.put(type, cachedValue);
    }
    final Map<VirtualFile, List<Facet>> value = cachedValue.getValue();
    LOG.assertTrue(value != null);
    return value;
  }

  @NotNull
  private <F extends Facet&FacetRootsProvider> Map<VirtualFile, List<Facet>> computeRootToFacetsMap(final FacetTypeId<F> type) {
    final Module[] modules = myModuleManager.getModules();
    final HashMap<VirtualFile, List<Facet>> map = new HashMap<>();
    for (Module module : modules) {
      final Collection<F> facets = FacetManager.getInstance(module).getFacetsByType(type);
      for (F facet : facets) {
        for (VirtualFile root : facet.getFacetRoots()) {
          List<Facet> list = map.get(root);
          if (list == null) {
            list = new SmartList<>();
            map.put(root, list);
          }
          list.add(facet);
        }
      }
    }
    return map;
  }

  @Override
  @Nullable
  public <F extends Facet & FacetRootsProvider> F findFacet(VirtualFile file, FacetTypeId<F> type) {
    final List<F> list = findFacets(file, type);
    return list.size() > 0 ? list.get(0) : null;
  }

  @Override
  @NotNull
  public <F extends Facet & FacetRootsProvider> List<F> findFacets(VirtualFile file, FacetTypeId<F> type) {
    final Map<VirtualFile, List<Facet>> map = getRootToFacetsMap(type);
    if (!map.isEmpty()) {
      while (file != null) {
        final List<F> list = (List<F>)((List)map.get(file));
        if (list != null) {
          return list;
        }
        file = file.getParent();
      }
    }
    return Collections.emptyList();
  }

  private static class AllFacetsOfTypeModificationTracker<F extends Facet> extends SimpleModificationTracker implements Disposable, ProjectWideFacetListener<F> {
    AllFacetsOfTypeModificationTracker(final Project project, final FacetTypeId<F> type) {
      ProjectWideFacetListenersRegistry.getInstance(project).registerListener(type, this, this);
    }

    @Override
    public void facetAdded(@NotNull final F facet) {
      incModificationCount();
    }

    @Override
    public void facetRemoved(@NotNull final F facet) {
      incModificationCount();
    }

    @Override
    public void facetConfigurationChanged(@NotNull final F facet) {
      incModificationCount();
    }

    @Override
    public void firstFacetAdded() {

    }

    @Override
    public void beforeFacetRemoved(@NotNull F facet) {

    }

    @Override
    public void allFacetsRemoved() {

    }

    @Override
    public void dispose() {
    }
  }
}
