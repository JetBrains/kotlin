// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.module.impl;

import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ModuleTypeManagerImpl extends ModuleTypeManager {
  private static final Logger LOG = Logger.getInstance(ModuleTypeManagerImpl.class);
  @ApiStatus.Internal
  public static final ExtensionPointName<ModuleTypeEP> EP_NAME = new ExtensionPointName<>("com.intellij.moduleType");

  private final LinkedHashMap<ModuleType<?>, Boolean> myModuleTypes = new LinkedHashMap<>();

  public ModuleTypeManagerImpl() {
    registerModuleType(getDefaultModuleType(), true);
    EP_NAME.processWithPluginDescriptor((ep, pluginDescriptor) -> {
      if (ep.id == null) {
        LOG.error(new PluginException("'id' attribute isn't specified for <moduleType implementationClass='" + ep.implementationClass + "'> extension", pluginDescriptor.getPluginId()));
      }
    });
  }

  @Override
  public void registerModuleType(ModuleType type) {
    registerModuleType(type, false);
  }

  @Override
  public void unregisterModuleType(ModuleType<?> type) {
    myModuleTypes.remove(type);
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
    for (ModuleTypeEP moduleTypeEP : EP_NAME.getExtensionList()) {
      ModuleType<?> moduleType = moduleTypeEP.getModuleType();
      if (!myModuleTypes.containsKey(moduleType)) {
        result.add(moduleType);
      }
    }
    return result.toArray(new ModuleType[0]);
  }

  @Override
  public ModuleType<?> findByID(@Nullable String moduleTypeId) {
    if (moduleTypeId == null) {
      return getDefaultModuleType();
    }

    for (ModuleType<?> type : myModuleTypes.keySet()) {
      if (type.getId().equals(moduleTypeId)) {
        return type;
      }
    }

    ModuleTypeEP result = EP_NAME.getByKey(moduleTypeId, ModuleTypeManagerImpl.class, it -> it.id);
    if (result != null) {
      return result.getModuleType();
    }
    return new UnknownModuleType(moduleTypeId, getDefaultModuleType());
  }

  @Override
  public boolean isClasspathProvider(@NotNull ModuleType moduleType) {
    for (ModuleTypeEP ep : EP_NAME.getExtensionList()) {
      if (moduleType.getId().equals(ep.id)) {
        return ep.classpathProvider;
      }
    }

    Boolean provider = myModuleTypes.get(moduleType);
    return provider != null && provider;
  }

  @Override
  public ModuleType<?> getDefaultModuleType() {
    return EmptyModuleType.getInstance();
  }
}
