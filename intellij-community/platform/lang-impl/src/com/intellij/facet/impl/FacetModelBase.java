// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.facet.impl;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetModel;
import com.intellij.facet.FacetTypeId;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class FacetModelBase implements FacetModel {
  private volatile Map<FacetTypeId<?>, Collection<Facet<?>>> myType2Facets;
  private volatile Map<FacetAndType, Collection<Facet<?>>> myChildFacets;
  private volatile Facet<?>[] mySortedFacets;

  @Override
  public Facet<?> @NotNull [] getSortedFacets() {
    if (mySortedFacets == null) {
      final Facet<?>[] allFacets = getAllFacets();
      if (allFacets.length == 0) {
        mySortedFacets = Facet.EMPTY_ARRAY;
      }
      else {
        LinkedHashSet<Facet<?>> facets = new LinkedHashSet<>();
        for (Facet<?> facet : allFacets) {
          addUnderlyingFacets(facets, facet);
        }
        mySortedFacets = facets.toArray(Facet.EMPTY_ARRAY);
      }
    }
    return mySortedFacets;
  }

  private static void addUnderlyingFacets(final LinkedHashSet<? super Facet<?>> facets, final Facet<?> facet) {
    final Facet<?> underlyingFacet = facet.getUnderlyingFacet();
    if (underlyingFacet != null && !facets.contains(facet)) {
      addUnderlyingFacets(facets, underlyingFacet);
    }
    facets.add(facet);
  }

  @Override
  @NotNull
  public <F extends Facet<?>> Collection<F> getFacetsByType(@NotNull Facet<?> underlyingFacet, FacetTypeId<F> typeId) {
    if (myChildFacets == null) {
      MultiMap<FacetAndType, Facet<?>> children = new MultiMap<>();
      for (Facet<?> facet : getAllFacets()) {
        Facet<?> underlying = facet.getUnderlyingFacet();
        if (underlying != null) {
          children.putValue(new FacetAndType(underlying,  facet.getTypeId()), facet);
        }
      }
      myChildFacets = children.freezeValues();
    }
    //noinspection unchecked
    Collection<F> facets = (Collection<F>)myChildFacets.get(new FacetAndType(underlyingFacet, typeId));
    return facets != null ? facets : Collections.emptyList();
  }

  @Override
  @NotNull
  public String getFacetName(@NotNull Facet<?> facet) {
    return facet.getName();
  }

  @Override
  @Nullable
  public <F extends Facet<?>> F findFacet(final FacetTypeId<F> type, final String name) {
    final Collection<F> fs = getFacetsByType(type);
    for (F f : fs) {
      if (f.getName().equals(name)) {
        return f;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public <F extends Facet<?>> F getFacetByType(@NotNull final Facet<?> underlyingFacet, final FacetTypeId<F> typeId) {
    final Collection<F> fs = getFacetsByType(underlyingFacet, typeId);
    return fs.isEmpty() ? null : fs.iterator().next();
  }

  @Override
  @Nullable
  public <F extends Facet<?>> F getFacetByType(FacetTypeId<F> typeId) {
    final Collection<F> facets = getFacetsByType(typeId);
    return facets.isEmpty() ? null : facets.iterator().next();
  }

  @Override
  @NotNull
  public <F extends Facet<?>> Collection<F> getFacetsByType(FacetTypeId<F> typeId) {
    if (myType2Facets == null) {
      MultiMap<FacetTypeId<?>, Facet<?>> typeToFacets = new MultiMap<>();
      for (Facet<?> facet : getAllFacets()) {
        typeToFacets.putValue(facet.getTypeId(), facet);
      }
      myType2Facets = typeToFacets.freezeValues();
    }

    @SuppressWarnings("unchecked")
    Collection<F> facets = (Collection<F>)myType2Facets.get(typeId);
    return facets != null ? facets : Collections.emptyList();
  }

  protected void facetsChanged() {
    myChildFacets = null;
    myType2Facets = null;
    mySortedFacets = null;
  }

  private static final class FacetAndType {
    final Facet<?> myFacet;
    final FacetTypeId<?> myTypeId;

    private FacetAndType(Facet<?> facet, FacetTypeId<?> id) {
      myFacet = facet;
      myTypeId = id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FacetAndType type = (FacetAndType)o;
      return Objects.equals(myFacet, type.myFacet) &&
             Objects.equals(myTypeId, type.myTypeId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myFacet, myTypeId);
    }
  }
}
