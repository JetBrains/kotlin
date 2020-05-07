// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl;

import com.intellij.facet.ProjectFacetListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginAware;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.RequiredElement;
import com.intellij.serviceContainer.LazyExtensionInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectFacetListenerEP extends LazyExtensionInstance<ProjectFacetListener<?>> implements PluginAware {
  @Attribute("facet-type")
  @RequiredElement
  public String myFacetTypeId;

  @Attribute("implementation")
  @RequiredElement
  public String myImplementationClass;
  private PluginDescriptor myPluginDescriptor;

  public String getFacetTypeId() {
    return myFacetTypeId;
  }

  @Override
  protected @Nullable String getImplementationClassName() {
    return myImplementationClass;
  }

  public ProjectFacetListener<?> getListenerInstance() {
    return getInstance(ApplicationManager.getApplication(), myPluginDescriptor);
  }

  @Override
  public void setPluginDescriptor(@NotNull PluginDescriptor pluginDescriptor) {
    myPluginDescriptor = pluginDescriptor;
  }
}
