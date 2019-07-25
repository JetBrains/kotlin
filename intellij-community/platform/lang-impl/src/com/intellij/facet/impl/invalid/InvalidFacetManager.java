/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.facet.impl.invalid;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author nik
 */
public abstract class InvalidFacetManager {
  public static InvalidFacetManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, InvalidFacetManager.class);
  }

  public abstract boolean isIgnored(@NotNull InvalidFacet facet);
  public abstract void setIgnored(@NotNull InvalidFacet facet, boolean ignored);

  public abstract List<InvalidFacet> getInvalidFacets();
}
