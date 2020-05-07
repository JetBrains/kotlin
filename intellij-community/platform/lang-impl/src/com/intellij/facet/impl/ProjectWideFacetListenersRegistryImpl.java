// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl;

import com.intellij.facet.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

final class ProjectWideFacetListenersRegistryImpl extends ProjectWideFacetListenersRegistry {
  private final Project myProject;

  ProjectWideFacetListenersRegistryImpl(@NotNull Project project) {
    myProject = project;
  }

  @Override
  public <F extends Facet<?>> void registerListener(@NotNull FacetTypeId<F> typeId, @NotNull ProjectWideFacetListener<? extends F> listener) {
    FacetEventsPublisher.getInstance(myProject).registerListener(typeId, new ProjectWideFacetListenerWrapper<>(listener));
  }

  @Override
  public <F extends Facet<?>> void unregisterListener(@NotNull FacetTypeId<F> typeId, @NotNull ProjectWideFacetListener<? extends F> listener) {
    FacetEventsPublisher.getInstance(myProject).unregisterListener(typeId, new ProjectWideFacetListenerWrapper<>(listener));
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
    FacetEventsPublisher.getInstance(myProject).registerListener(null, new ProjectWideFacetListenerWrapper<>(listener));
  }

  @Override
  public void unregisterListener(@NotNull final ProjectWideFacetListener<Facet> listener) {
    FacetEventsPublisher.getInstance(myProject).registerListener(null, new ProjectWideFacetListenerWrapper<>(listener));
  }

  @Override
  public void registerListener(@NotNull final ProjectWideFacetListener<Facet> listener, @NotNull final Disposable parentDisposable) {
    registerListener(listener);
    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        unregisterListener(listener);
      }
    });
  }

  private static class ProjectWideFacetListenerWrapper<F extends Facet<?>> implements ProjectFacetListener<F> {
    private final @NotNull ProjectWideFacetListener<F> myListener;

    private ProjectWideFacetListenerWrapper(@NotNull ProjectWideFacetListener<F> listener) {
      myListener = listener;
    }

    @Override
    public void firstFacetAdded(@NotNull Project project) {
      myListener.firstFacetAdded();
    }

    @Override
    public void facetAdded(@NotNull F facet) {
      myListener.facetAdded(facet);
    }

    @Override
    public void beforeFacetRemoved(@NotNull F facet) {
      myListener.beforeFacetRemoved(facet);
    }

    @Override
    public void facetRemoved(@NotNull F facet, @NotNull Project project) {
      myListener.facetRemoved(facet);
    }

    @Override
    public void allFacetsRemoved(@NotNull Project project) {
      myListener.allFacetsRemoved();
    }

    @Override
    public void facetConfigurationChanged(@NotNull F facet) {
      myListener.facetConfigurationChanged(facet);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      return myListener.equals(((ProjectWideFacetListenerWrapper<?>)o).myListener);
    }

    @Override
    public int hashCode() {
      return myListener.hashCode();
    }
  }
}
