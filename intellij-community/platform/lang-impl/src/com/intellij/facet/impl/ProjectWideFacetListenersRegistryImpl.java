// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl;

import com.intellij.ProjectTopics;
import com.intellij.facet.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

final class ProjectWideFacetListenersRegistryImpl extends ProjectWideFacetListenersRegistry {
  private final Map<FacetTypeId<?>, EventDispatcher<ProjectWideFacetListener>> myDispatchers = new HashMap<>();
  private final Map<FacetTypeId<?>, Map<Facet<?>, Boolean>> myFacetsByType = new HashMap<>();
  private final EventDispatcher<ProjectWideFacetListener> myAllFacetsListener = EventDispatcher.create(ProjectWideFacetListener.class);

  ProjectWideFacetListenersRegistryImpl(@NotNull Project project) {
    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(ProjectTopics.MODULES, new ModuleListener() {
      @Override
      public void moduleAdded(@NotNull Project project, @NotNull Module module) {
        onModuleAdded(module);
      }

      @Override
      public void beforeModuleRemoved(@NotNull Project project, @NotNull Module module) {
        for (Facet<?> facet : FacetManager.getInstance(module).getAllFacets()) {
          onFacetRemoved(facet, true);
        }
      }

      @Override
      public void moduleRemoved(@NotNull Project project, @NotNull Module module) {
        onModuleRemoved(module);
      }
    });

    connection.subscribe(FacetManager.FACETS_TOPIC, new FacetManagerAdapter() {
      @Override
      public void facetAdded(@NotNull Facet facet) {
        onFacetAdded(facet);
      }

      @Override
      public void beforeFacetRemoved(@NotNull Facet facet) {
        onFacetRemoved(facet, true);
      }

      @Override
      public void facetRemoved(@NotNull Facet facet) {
        onFacetRemoved(facet, false);
      }

      @Override
      public void facetConfigurationChanged(@NotNull Facet facet) {
        onFacetChanged(facet);
      }
    });

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      onModuleAdded(module);
    }
  }

  private void onModuleRemoved(@NotNull Module module) {
    for (Facet<?> facet : FacetManager.getInstance(module).getAllFacets()) {
      onFacetRemoved(facet, false);
    }
  }

  private void onModuleAdded(@NotNull Module module) {
    for (Facet<?> facet : FacetManager.getInstance(module).getAllFacets()) {
      onFacetAdded(facet);
    }
  }

  private void onFacetRemoved(@NotNull Facet<?> facet, final boolean before) {
    final FacetTypeId<?> typeId = facet.getTypeId();
    Map<Facet<?>, Boolean> facets = myFacetsByType.get(typeId);
    boolean lastFacet;
    if (facets != null) {
      facets.remove(facet);
      lastFacet = facets.isEmpty();
      if (lastFacet) {
        myFacetsByType.remove(typeId);
      }
    }
    else {
      lastFacet = true;
    }
    EventDispatcher<ProjectWideFacetListener> dispatcher = myDispatchers.get(typeId);
    if (dispatcher != null) {
      if (before) {
        //noinspection unchecked
        dispatcher.getMulticaster().beforeFacetRemoved(facet);
      }
      else {
        //noinspection unchecked
        dispatcher.getMulticaster().facetRemoved(facet);
        if (lastFacet) {
          dispatcher.getMulticaster().allFacetsRemoved();
        }
      }
    }

    if (before) {
      getAllFacetsMulticaster().beforeFacetRemoved(facet);
    }
    else {
      getAllFacetsMulticaster().facetRemoved(facet);
      if (myFacetsByType.isEmpty()) {
        getAllFacetsMulticaster().allFacetsRemoved();
      }
    }
  }

  private ProjectWideFacetListener<Facet<?>> getAllFacetsMulticaster() {
    //noinspection unchecked
    return myAllFacetsListener.getMulticaster();
  }

  private void onFacetAdded(@NotNull Facet<?> facet) {
    boolean firstFacet = myFacetsByType.isEmpty();
    final FacetTypeId<?> typeId = facet.getTypeId();
    Map<Facet<?>, Boolean> facets = myFacetsByType.get(typeId);
    if (facets == null) {
      facets = ContainerUtil.createWeakMap();
      myFacetsByType.put(typeId, facets);
    }
    boolean firstFacetOfType = facets.isEmpty();
    facets.put(facet, true);

    if (firstFacet) {
      getAllFacetsMulticaster().firstFacetAdded();
    }
    getAllFacetsMulticaster().facetAdded(facet);

    final EventDispatcher<ProjectWideFacetListener> dispatcher = myDispatchers.get(typeId);
    if (dispatcher != null) {
      if (firstFacetOfType) {
        dispatcher.getMulticaster().firstFacetAdded();
      }
      //noinspection unchecked
      dispatcher.getMulticaster().facetAdded(facet);
    }
  }

  private void onFacetChanged(@NotNull Facet<?> facet) {
    final EventDispatcher<ProjectWideFacetListener> dispatcher = myDispatchers.get(facet.getTypeId());
    if (dispatcher != null) {
      //noinspection unchecked
      dispatcher.getMulticaster().facetConfigurationChanged(facet);
    }
    getAllFacetsMulticaster().facetConfigurationChanged(facet);
  }

  @Override
  public <F extends Facet<?>> void registerListener(@NotNull FacetTypeId<F> typeId, @NotNull ProjectWideFacetListener<? extends F> listener) {
    EventDispatcher<ProjectWideFacetListener> dispatcher = myDispatchers.get(typeId);
    if (dispatcher == null) {
      dispatcher = EventDispatcher.create(ProjectWideFacetListener.class);
      myDispatchers.put(typeId, dispatcher);
    }
    dispatcher.addListener(listener);
  }

  @Override
  public <F extends Facet<?>> void unregisterListener(@NotNull FacetTypeId<F> typeId, @NotNull ProjectWideFacetListener<? extends F> listener) {
    final EventDispatcher<ProjectWideFacetListener> dispatcher = myDispatchers.get(typeId);
    if (dispatcher != null) {
      dispatcher.removeListener(listener);
    }
  }

  @Override
  public <F extends Facet<?>> void registerListener(@NotNull final FacetTypeId<F> typeId, @NotNull final ProjectWideFacetListener<? extends F> listener,
                                                 @NotNull final Disposable parentDisposable) {
    registerListener(typeId, listener);
    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        unregisterListener(typeId, listener);
      }
    });
  }

  @Override
  public void registerListener(@NotNull final ProjectWideFacetListener<Facet> listener) {
    myAllFacetsListener.addListener(listener);
  }

  @Override
  public void unregisterListener(@NotNull final ProjectWideFacetListener<Facet> listener) {
    myAllFacetsListener.removeListener(listener);
  }

  @Override
  public void registerListener(@NotNull final ProjectWideFacetListener<Facet> listener, @NotNull final Disposable parentDisposable) {
    myAllFacetsListener.addListener(listener, parentDisposable);
  }
}
