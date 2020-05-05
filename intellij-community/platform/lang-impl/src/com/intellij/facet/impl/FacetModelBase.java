// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.facet.impl;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetModel;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.util.MultiValuesMap;
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
      MultiValuesMap<FacetAndType, Facet<?>> children = new MultiValuesMap<>();
      for (Facet<?> facet : getAllFacets()) {
        final Facet<?> underlying = facet.getUnderlyingFacet();
        if (underlying != null) {
          children.put(new FacetAndType(underlying,  facet.getTypeId()), facet);
        }
      }

      Map<FacetAndType, Collection<Facet<?>>> childFacets = new HashMap<>();
      for (FacetAndType pair : children.keySet()) {
        final Collection<Facet<?>> facets = children.get(pair);
        assert facets != null;
        childFacets.put(pair, Collections.unmodifiableCollection(facets));
      }
      myChildFacets = childFacets;
    }
    //noinspection unchecked
    final Collection<F> facets = (Collection<F>)myChildFacets.get(new FacetAndType(underlyingFacet, typeId));
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
      MultiValuesMap<FacetTypeId<?>, Facet<?>> typeToFacets = new MultiValuesMap<>();
      for (Facet<?> facet : getAllFacets()) {
        typeToFacets.put(facet.getTypeId(), facet);
      }
      Map<FacetTypeId<?>, Collection<Facet<?>>> typeToFacetsCollection = new HashMap<>();
      for (FacetTypeId<?> id : typeToFacets.keySet()) {
        final Collection<Facet<?>> facets = typeToFacets.get(id);
        typeToFacetsCollection.put(id, Collections.unmodifiableCollection(facets));
      }
      myType2Facets = typeToFacetsCollection;
    }

    final Collection<F> facets = (Collection<F>)myType2Facets.get(typeId);
    return facets != null ? facets : Collections.emptyList();
  }

  protected void facetsChanged() {
    myChildFacets = null;
    myType2Facets = null;
    mySortedFacets = null;
  }
  
  private static class FacetAndType {
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
