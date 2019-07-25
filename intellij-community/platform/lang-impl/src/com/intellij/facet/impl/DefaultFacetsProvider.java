/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.facet.impl;

import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author nik
 */
public class DefaultFacetsProvider implements FacetsProvider {
  public static final FacetsProvider INSTANCE = new DefaultFacetsProvider();

  @Override
  @NotNull
  public Facet[] getAllFacets(Module module) {
    return FacetManager.getInstance(module).getAllFacets();
  }

  @Override
  @NotNull
  public <F extends Facet> Collection<F> getFacetsByType(Module module, FacetTypeId<F> type) {
    return FacetManager.getInstance(module).getFacetsByType(type);
  }

  @Override
  @Nullable
  public <F extends Facet> F findFacet(Module module, FacetTypeId<F> type, String name) {
    return FacetManager.getInstance(module).findFacet(type, name);
  }
}
