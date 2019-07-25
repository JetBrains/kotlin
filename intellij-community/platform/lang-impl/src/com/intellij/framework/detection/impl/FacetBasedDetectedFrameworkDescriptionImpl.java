/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.framework.detection.impl;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetTypeId;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * @author nik
 */
public class FacetBasedDetectedFrameworkDescriptionImpl<F extends Facet, C extends FacetConfiguration> extends FacetBasedDetectedFrameworkDescription<F, C> {
  private final Module myModule;

  public FacetBasedDetectedFrameworkDescriptionImpl(@NotNull Module module,
                                                    FacetBasedFrameworkDetector<F, C> detector, @NotNull C configuration,
                                                    Set<VirtualFile> files) {
    super(detector, configuration, files);
    myModule = module;
  }

  @Override
  protected String getModuleName() {
    return myModule.getName();
  }

  @Override
  public void setupFramework(@NotNull ModifiableModelsProvider modifiableModelsProvider, @NotNull ModulesProvider modulesProvider) {
    doSetup(modifiableModelsProvider, myModule);
  }

  @Override
  @NotNull
  protected Collection<? extends Facet> getExistentFacets(FacetTypeId<?> underlyingFacetType) {
    return FacetManager.getInstance(myModule).getFacetsByType(underlyingFacetType);
  }
}
