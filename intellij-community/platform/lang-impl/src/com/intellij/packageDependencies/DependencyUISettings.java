// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packageDependencies;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.packageDependencies.ui.PatternDialectProvider;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(name = "DependencyUISettings", storages = @Storage("ui.lnf.xml"))
public class DependencyUISettings implements PersistentStateComponent<DependencyUISettings> {
  public boolean UI_FLATTEN_PACKAGES = true;
  public boolean UI_SHOW_FILES = true;
  public boolean UI_SHOW_MODULES = true;
  public boolean UI_SHOW_MODULE_GROUPS = true;
  public boolean UI_FILTER_LEGALS = false;
  public boolean UI_FILTER_OUT_OF_CYCLE_PACKAGES = true;
  public boolean UI_GROUP_BY_SCOPE_TYPE = true;
  public boolean UI_COMPACT_EMPTY_MIDDLE_PACKAGES = true;
  public String SCOPE_TYPE = PatternDialectProvider.EP_NAME.getExtensionList().get(0).getShortName();

  public static DependencyUISettings getInstance() {
    return ServiceManager.getService(DependencyUISettings.class);
  }

  @Override
  public DependencyUISettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull DependencyUISettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}