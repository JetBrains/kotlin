// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManagerImpl;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ProjectModelExternalSource;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * This class isn't used in the new implementation of project model, which is based on {@link com.intellij.workspaceModel.ide Workspace Model}.
 * It shouldn't be used directly, its interface {@link FacetModel} should be used instead.
 */
@ApiStatus.Internal
public final class FacetModelImpl extends FacetModelBase implements ModifiableFacetModel {
  private static final Logger LOG = Logger.getInstance(FacetModelImpl.class);
  private final List<Facet<?>> myFacets = new ArrayList<>();
  private final Map<Facet<?>, String> myFacet2NewName = new HashMap<>();
  private final FacetManagerImpl myManager;
  private final List<Listener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public FacetModelImpl(@NotNull FacetManagerImpl manager) {
    myManager = manager;
  }

  public void addFacetsFromManager() {
    for (Facet<?> facet : myManager.getAllFacets()) {
      addFacet(facet);
    }
  }

  @Override
  public void addFacet(Facet<?> facet) {
    if (myFacets.contains(facet)) {
      LOG.error("Facet " + facet + " [" + facet.getTypeId() + "] is already added");
    }

    myFacets.add(facet);
    facetsChanged();
  }

  @Override
  public void addFacet(Facet<?> facet, @Nullable ProjectModelExternalSource externalSource) {
    addFacet(facet);
    FacetManagerImpl.setExternalSource(facet, externalSource);
  }

  @Override
  public void removeFacet(Facet<?> facet) {
    if (!myFacets.remove(facet)) {
      LOG.error("Facet " + facet + " [" + facet.getTypeId() + "] not found");
    }
    myFacet2NewName.remove(facet);
    facetsChanged();
  }

  @Override
  public void replaceFacet(@NotNull Facet<?> original, @NotNull Facet<?> replacement) {
    int index = myFacets.indexOf(original);
    if (index != -1) {
      myFacets.set(index, replacement);
      facetsChanged();
    }
  }

  @Override
  public void rename(final Facet<?> facet, final String newName) {
    if (!newName.equals(facet.getName())) {
      myFacet2NewName.put(facet, newName);
    } else {
      myFacet2NewName.remove(facet);
    }
    facetsChanged();
  }

  @Override
  @Nullable
  public String getNewName(final Facet<?> facet) {
    return myFacet2NewName.get(facet);
  }

  @Override
  public void commit() {
    myManager.commit(this);
  }

  @Override
  public boolean isModified() {
    return !new HashSet<>(myFacets).equals(ContainerUtil.set(myManager.getAllFacets())) || !myFacet2NewName.isEmpty();
  }

  @Override
  public boolean isNewFacet(final Facet<?> facet) {
    return myFacets.contains(facet) && ArrayUtil.find(myManager.getAllFacets(), facet) == -1;
  }

  @Override
  public Facet<?> @NotNull [] getAllFacets() {
    return myFacets.toArray(Facet.EMPTY_ARRAY);
  }

  @Override
  @NotNull
  public String getFacetName(@NotNull final Facet<?> facet) {
    return myFacet2NewName.containsKey(facet) ? myFacet2NewName.get(facet) : facet.getName();
  }

  @Override
  public void addListener(@NotNull final Listener listener, @NotNull Disposable parentDisposable) {
    myListeners.add(listener);
    Disposer.register(parentDisposable, () -> myListeners.remove(listener));
  }

  @Override
  protected void facetsChanged() {
    super.facetsChanged();
    for (Listener each : myListeners) {
      each.onChanged();
    }
  }
}
