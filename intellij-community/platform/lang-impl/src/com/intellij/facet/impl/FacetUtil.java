// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.facet.impl;

import com.intellij.configurationStore.ComponentSerializationUtil;
import com.intellij.facet.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.facet.JpsFacetSerializer;

import java.util.Arrays;

/**
 * @author nik
 */
public class FacetUtil {

  public static <F extends Facet> F addFacet(Module module, FacetType<F, ?> type) {
    final ModifiableFacetModel model = FacetManager.getInstance(module).createModifiableModel();
    final F facet = createFacet(module, type);
    ApplicationManager.getApplication().runWriteAction(() -> {
      model.addFacet(facet);
      model.commit();
    });
    return facet;
  }

  private static <F extends Facet, C extends FacetConfiguration> F createFacet(final Module module, final FacetType<F, C> type) {
    return FacetManager.getInstance(module).createFacet(type, type.getPresentableName(), type.createDefaultConfiguration(), null);
  }

  public static void deleteFacet(final Facet facet) {
    WriteAction.runAndWait(() -> {
      if (!isRegistered(facet)) {
        return;
      }

      ModifiableFacetModel model = FacetManager.getInstance(facet.getModule()).createModifiableModel();
      model.removeFacet(facet);
      model.commit();
    });
  }

  public static boolean isRegistered(Facet facet) {
    return Arrays.asList(FacetManager.getInstance(facet.getModule()).getAllFacets()).contains(facet);
  }

  public static void loadFacetConfiguration(final @NotNull FacetConfiguration configuration, final @Nullable Element config)
      throws InvalidDataException {
    if (config != null) {
      if (configuration instanceof PersistentStateComponent) {
        ComponentSerializationUtil.loadComponentState((PersistentStateComponent)configuration, config);
      }
      else {
        configuration.readExternal(config);
      }
    }
  }

  public static Element saveFacetConfiguration(final FacetConfiguration configuration) throws WriteExternalException {
    if (configuration instanceof PersistentStateComponent) {
      Object state = ((PersistentStateComponent)configuration).getState();
      if (state instanceof Element) return ((Element)state);
      return XmlSerializer.serialize(state, new SkipDefaultValuesSerializationFilters());
    }
    else {
      final Element config = new Element(JpsFacetSerializer.CONFIGURATION_TAG);
      configuration.writeExternal(config);
      return config;
    }
  }
}
