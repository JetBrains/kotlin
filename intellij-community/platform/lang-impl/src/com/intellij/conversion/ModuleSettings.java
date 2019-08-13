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

package com.intellij.conversion;

import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public interface ModuleSettings extends ComponentManagerSettings {

  @NonNls String MODULE_ROOT_MANAGER_COMPONENT = "NewModuleRootManager";

  @NotNull
  String getModuleName();

  @Nullable
  String getModuleType();

  @NotNull
  File getModuleFile();

  @NotNull
  Collection<? extends Element> getFacetElements(@NotNull String facetTypeId);

  @Nullable
  Element getFacetElement(@NotNull String facetTypeId);

  void setModuleType(@NotNull String moduleType);

  @NotNull
  String expandPath(@NotNull String path);

  @NotNull
  String collapsePath(@NotNull String path);

  @NotNull
  Collection<File> getSourceRoots(boolean includeTests);

  @NotNull
  Collection<File> getContentRoots();

  String getProjectOutputUrl();

  void addExcludedFolder(@NotNull File directory);

  @NotNull
  List<File> getModuleLibraryRoots(String libraryName);

  @NotNull
  Collection<ModuleSettings> getAllModuleDependencies();

  boolean hasModuleLibrary(String libraryName);

  List<Element> getOrderEntries();

  void addFacetElement(@NotNull String facetTypeId, @NotNull String facetName, Element configuration);
}
