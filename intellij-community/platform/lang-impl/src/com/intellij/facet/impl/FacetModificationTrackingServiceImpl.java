// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.facet.impl;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetManagerAdapter;
import com.intellij.facet.FacetModificationTrackingService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.ModificationTrackerListener;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FacetModificationTrackingServiceImpl extends FacetModificationTrackingService {
  private final ConcurrentMap<Facet<?>, Pair<SimpleModificationTracker, EventDispatcher<ModificationTrackerListener>>> myModificationsTrackers =
    new ConcurrentHashMap<>();

  public FacetModificationTrackingServiceImpl(final Module module) {
    module.getMessageBus().connect().subscribe(FacetManager.FACETS_TOPIC, new FacetModificationTrackingListener());
  }

  @Override
  public @NotNull ModificationTracker getFacetModificationTracker(final @NotNull Facet<?> facet) {
    return getFacetInfo(facet).first;
  }

  private Pair<SimpleModificationTracker, EventDispatcher<ModificationTrackerListener>> getFacetInfo(final Facet<?> facet) {
    Pair<SimpleModificationTracker, EventDispatcher<ModificationTrackerListener>> pair = myModificationsTrackers.get(facet);
    if (pair != null) return pair;

    myModificationsTrackers.putIfAbsent(facet, Pair.create(new SimpleModificationTracker(), EventDispatcher.create(ModificationTrackerListener.class)));
    return myModificationsTrackers.get(facet);
  }

  @Override
  public void incFacetModificationTracker(final @NotNull Facet<?> facet) {
    final Pair<SimpleModificationTracker, EventDispatcher<ModificationTrackerListener>> pair = getFacetInfo(facet);
    pair.first.incModificationCount();
    //noinspection unchecked
    pair.second.getMulticaster().modificationCountChanged(facet);
  }

  @Override
  public <T extends Facet<?>> void addModificationTrackerListener(final T facet, final ModificationTrackerListener<? super T> listener, final Disposable parent) {
    getFacetInfo(facet).second.addListener(listener, parent);
  }

  @Override
  public void removeModificationTrackerListener(final Facet<?> facet, final ModificationTrackerListener<?> listener) {
    getFacetInfo(facet).second.removeListener(listener);
  }

  private class FacetModificationTrackingListener extends FacetManagerAdapter {
    @Override
    public void facetConfigurationChanged(final @NotNull Facet facet) {
      incFacetModificationTracker(facet);
    }

    @Override
    public void facetRemoved(final @NotNull Facet facet) {
      myModificationsTrackers.remove(facet);
    }
  }
}
