// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.module.impl;

import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ModuleTypeManagerImpl extends ModuleTypeManager {
  private static final Logger LOG = Logger.getInstance(ModuleTypeManagerImpl.class);

  private final LinkedHashMap<ModuleType<?>, Boolean> myModuleTypes = new LinkedHashMap<>();

  public ModuleTypeManagerImpl() {
    registerModuleType(getDefaultModuleType(), true);
    for (ModuleTypeEP ep : ModuleTypeEP.EP_NAME.getExtensions()) {
      if (ep.id == null) {
        LOG.error(new PluginException("'id' attribute isn't specified for <moduleType implementationClass='" + ep.implementationClass + "'> extension", ep.getPluginId()));
      }
    }
  }

  @Override
  public void registerModuleType(ModuleType type) {
    registerModuleType(type, false);
  }

  @Override
  public void registerModuleType(ModuleType type, boolean classpathProvider) {
    for (ModuleType<?> oldType : myModuleTypes.keySet()) {
      if (oldType.getId().equals(type.getId())) {
        PluginException.logPluginError(LOG, "Trying to register a module type that clashes with existing one. Old=" + oldType + ", new = " + type, null, type.getClass());
        return;
      }
    }

    myModuleTypes.put(type, classpathProvider);
  }

  @Override
  public ModuleType<?>[] getRegisteredTypes() {
    List<ModuleType<?>> result = new ArrayList<>(myModuleTypes.keySet());
    for (ModuleTypeEP moduleTypeEP : ModuleTypeEP.EP_NAME.getExtensionList()) {
      ModuleType<?> moduleType = moduleTypeEP.getModuleType();
      if (!myModuleTypes.containsKey(moduleType)) {
        result.add(moduleType);
      }
    }
    return result.toArray(new ModuleType[0]);
  }

  @Override
  public ModuleType<?> findByID(String moduleTypeID) {
    if (moduleTypeID == null) return getDefaultModuleType();
    for (ModuleType<?> type : myModuleTypes.keySet()) {
      if (type.getId().equals(moduleTypeID)) {
        return type;
      }
    }
    for (ModuleTypeEP ep : ModuleTypeEP.EP_NAME.getExtensionList()) {
      if (moduleTypeID.equals(ep.id)) {
        return ep.getModuleType();
      }
    }

    return new UnknownModuleType(moduleTypeID, getDefaultModuleType());
  }

  @Override
  public boolean isClasspathProvider(final ModuleType moduleType) {
    for (ModuleTypeEP ep : ModuleTypeEP.EP_NAME.getExtensionList()) {
      if (moduleType.getId().equals(ep.id)) {
        return ep.classpathProvider;
      }
    }

    final Boolean provider = myModuleTypes.get(moduleType);
    return provider != null && provider.booleanValue();
  }

  @Override
  public ModuleType<?> getDefaultModuleType() {
    return EmptyModuleType.getInstance();
  }
}
