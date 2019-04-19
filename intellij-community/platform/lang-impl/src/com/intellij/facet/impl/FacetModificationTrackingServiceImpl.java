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
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentMap;

/**
 * @author nik
 */
public class FacetModificationTrackingServiceImpl extends FacetModificationTrackingService {
  private final ConcurrentMap<Facet, Pair<SimpleModificationTracker, EventDispatcher<ModificationTrackerListener>>> myModificationsTrackers =
    ContainerUtil.newConcurrentMap();

  public FacetModificationTrackingServiceImpl(final Module module) {
    module.getMessageBus().connect().subscribe(FacetManager.FACETS_TOPIC, new FacetModificationTrackingListener());
  }

  @Override
  @NotNull
  public ModificationTracker getFacetModificationTracker(@NotNull final Facet facet) {
    return getFacetInfo(facet).first;
  }

  private Pair<SimpleModificationTracker, EventDispatcher<ModificationTrackerListener>> getFacetInfo(final Facet facet) {
    Pair<SimpleModificationTracker, EventDispatcher<ModificationTrackerListener>> pair = myModificationsTrackers.get(facet);
    if (pair != null) return pair;

    myModificationsTrackers.putIfAbsent(facet, Pair.create(new SimpleModificationTracker(), EventDispatcher.create(ModificationTrackerListener.class)));
    return myModificationsTrackers.get(facet);
  }

  @Override
  public void incFacetModificationTracker(@NotNull final Facet facet) {
    final Pair<SimpleModificationTracker, EventDispatcher<ModificationTrackerListener>> pair = getFacetInfo(facet);
    pair.first.incModificationCount();
    //noinspection unchecked
    pair.second.getMulticaster().modificationCountChanged(facet);
  }

  @Override
  public <T extends Facet> void addModificationTrackerListener(final T facet, final ModificationTrackerListener<? super T> listener, final Disposable parent) {
    getFacetInfo(facet).second.addListener(listener, parent);
  }

  @Override
  public void removeModificationTrackerListener(final Facet facet, final ModificationTrackerListener<?> listener) {
    getFacetInfo(facet).second.removeListener(listener);
  }

  private class FacetModificationTrackingListener extends FacetManagerAdapter {
    @Override
    public void facetConfigurationChanged(@NotNull final Facet facet) {
      incFacetModificationTracker(facet);
    }

    @Override
    public void facetRemoved(@NotNull final Facet facet) {
      myModificationsTrackers.remove(facet);
    }
  }
}
