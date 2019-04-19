/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class ProjectSettingsService {
  public static ProjectSettingsService getInstance(Project project) {
    return ServiceManager.getService(project, ProjectSettingsService.class);
  }

  public void openProjectSettings() {
  }

  public void openGlobalLibraries() {
  }

  public void openLibrary(@NotNull Library library) {
  }

  public void openModuleSettings(final Module module) {
  }

  public boolean canOpenModuleSettings() {
    return false;
  }

  public void openModuleLibrarySettings(final Module module) {
  }

  public boolean canOpenModuleLibrarySettings() {
    return false;
  }

  public void openContentEntriesSettings(final Module module) {
  }

  public boolean canOpenContentEntriesSettings() {
    return false;
  }

  public void openModuleDependenciesSettings(@NotNull Module module, @Nullable OrderEntry orderEntry) {
  }

  public boolean canOpenModuleDependenciesSettings() {
    return false;
  }

  public void openLibraryOrSdkSettings(@NotNull final OrderEntry orderEntry) {
    Configurable additionalSettingsConfigurable = getLibrarySettingsConfigurable(orderEntry);
    if (additionalSettingsConfigurable != null) {
      ShowSettingsUtil.getInstance().showSettingsDialog(orderEntry.getOwnerModule().getProject(),
                                                        additionalSettingsConfigurable.getDisplayName());
    }
  }

  public boolean canOpenLibraryOrSdkSettings(final OrderEntry orderEntry) {
    return getLibrarySettingsConfigurable(orderEntry) != null;
  }

  @Nullable
  private static Configurable getLibrarySettingsConfigurable(OrderEntry orderEntry) {
    if (!(orderEntry instanceof LibraryOrderEntry)) return null;
    LibraryOrderEntry libOrderEntry = (LibraryOrderEntry)orderEntry;
    Library lib = libOrderEntry.getLibrary();
    if (lib instanceof LibraryEx) {
      Project project = libOrderEntry.getOwnerModule().getProject();
      PersistentLibraryKind<?> libKind = ((LibraryEx)lib).getKind();
      if (libKind != null) {
        return LibrarySettingsProvider.getAdditionalSettingsConfigurable(project, libKind);
      }
    }
    return null;
  }

  public boolean processModulesMoved(final Module[] modules, @Nullable final ModuleGroup targetGroup) {
    return false;
  }

  public void showModuleConfigurationDialog(@Nullable String moduleToSelect, @Nullable String editorNameToSelect) {
  }

  public Sdk chooseAndSetSdk() {
    return null;
  }
}
