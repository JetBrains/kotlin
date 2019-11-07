// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl;

import com.intellij.facet.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author nik
 */
public class FacetTypeRegistryImpl extends FacetTypeRegistry {
  private static final Logger LOG = Logger.getInstance(FacetTypeRegistryImpl.class);
  private static final Comparator<FacetType> FACET_TYPE_COMPARATOR =
    (o1, o2) -> o1.getPresentableName().compareToIgnoreCase(o2.getPresentableName());
  private final Map<String, FacetTypeId> myTypeIds = new HashMap<>();
  private final Map<FacetTypeId, FacetType> myFacetTypes = new HashMap<>();
  private boolean myExtensionsLoaded = false;

  @Override
  public synchronized void registerFacetType(FacetType facetType) {
    final FacetTypeId typeId = facetType.getId();
    String id = facetType.getStringId();
    LOG.assertTrue(!id.contains("/"), "Facet type id '" + id + "' contains illegal character '/'");
    LOG.assertTrue(!myFacetTypes.containsKey(typeId), "Facet type '" + id + "' is already registered");
    myFacetTypes.put(typeId, facetType);

    LOG.assertTrue(!myTypeIds.containsKey(id), "Facet type id '" + id + "' is already registered");
    myTypeIds.put(id, typeId);
  }

  @Override
  public synchronized void unregisterFacetType(FacetType facetType) {
    final FacetTypeId id = facetType.getId();
    final String stringId = facetType.getStringId();
    LOG.assertTrue(myFacetTypes.remove(id) != null, "Facet type '" + stringId + "' is not registered");
    myFacetTypes.remove(id);
    myTypeIds.remove(stringId);
  }

  @NotNull
  @Override
  public synchronized FacetTypeId[] getFacetTypeIds() {
    loadExtensions();
    final Set<FacetTypeId> ids = myFacetTypes.keySet();
    return ids.toArray(new FacetTypeId[0]);
  }

  @NotNull
  @Override
  public synchronized FacetType[] getFacetTypes() {
    loadExtensions();
    final Collection<FacetType> types = myFacetTypes.values();
    final FacetType[] facetTypes = types.toArray(new FacetType[0]);
    Arrays.sort(facetTypes, FACET_TYPE_COMPARATOR);
    return facetTypes;
  }

  @NotNull
  @Override
  public FacetType[] getSortedFacetTypes() {
    final FacetType[] types = getFacetTypes();
    Arrays.sort(types, FACET_TYPE_COMPARATOR);
    return types;
  }

  @Override
  @Nullable
  public synchronized FacetType findFacetType(String id) {
    loadExtensions();
    final FacetTypeId typeId = myTypeIds.get(id);
    return typeId == null ? null : myFacetTypes.get(typeId);
  }

  @NotNull
  @Override
  public synchronized <F extends Facet<C>, C extends FacetConfiguration> FacetType<F, C> findFacetType(@NotNull FacetTypeId<F> typeId) {
    loadExtensions();
    FacetType type = myFacetTypes.get(typeId);
    LOG.assertTrue(type != null, "Cannot find facet by id '" + typeId + "'");
    //noinspection unchecked
    return type;
  }

  private void loadExtensions() {
    if (myExtensionsLoaded) {
      return;
    }

    FacetType.EP_NAME.getPoint(null).addExtensionPointListener(new ExtensionPointListener<FacetType>() {
      @Override
      public void extensionAdded(@NotNull final FacetType extension, @NotNull final PluginDescriptor pluginDescriptor) {
        registerFacetType(extension);
      }

      @Override
      public void extensionRemoved(@NotNull final FacetType extension, @NotNull final PluginDescriptor pluginDescriptor) {
        unregisterFacetType(extension);
      }
    }, true, null);
    myExtensionsLoaded = true;
  }
}
