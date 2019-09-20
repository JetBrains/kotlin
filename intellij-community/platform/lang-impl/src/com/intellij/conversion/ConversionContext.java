// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.conversion;

import com.intellij.openapi.components.StorageScheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public interface ConversionContext {
  @NotNull
  File getProjectBaseDir();

  /**
   * @return path to parent directory of .idea directory for directory-based storage scheme or path to ipr-file for file-based scheme
   */
  @NotNull
  File getProjectFile();

  @NotNull
  StorageScheme getStorageScheme();

  /**
   * @return .idea directory for directory based storage scheme or {@code null} for file-based scheme
   */
  File getSettingsBaseDir();

  ProjectSettings getProjectSettings() throws CannotConvertException;

  RunManagerSettings getRunManagerSettings() throws CannotConvertException;

  WorkspaceSettings getWorkspaceSettings() throws CannotConvertException;

  default ModuleSettings getModuleSettings(File moduleFile) throws CannotConvertException {
    return getModuleSettings(moduleFile.toPath());
  }

  ModuleSettings getModuleSettings(@NotNull Path moduleFile) throws CannotConvertException;

  @Nullable
  ModuleSettings getModuleSettings(@NotNull String moduleName);

  /**
   * @param fileName name of the file under .idea directory which contains the settings. For ipr-based storage format the settings will
   *                 be loaded from ipr-file
   * @return {@link ComponentManagerSettings} instance which can be used to read and modify the settings or {@code null} if the configuration
   * file cannot be loaded
   */
  @Nullable
  ComponentManagerSettings createProjectSettings(@NotNull String fileName);

  @NotNull
  String collapsePath(@NotNull String path);

  Collection<File> getLibraryClassRoots(@NotNull String name, @NotNull String level);

  @Nullable
  ComponentManagerSettings getCompilerSettings();

  @Nullable
  ComponentManagerSettings getProjectRootManagerSettings();

  @NotNull
  List<Path> getModulePaths() throws CannotConvertException;

  /**
   * @deprecated Use {@code #getModulePaths}.
   */
  @Deprecated
  File[] getModuleFiles();

  ComponentManagerSettings getModulesSettings();

  ProjectLibrariesSettings getProjectLibrariesSettings() throws CannotConvertException;

  ArtifactsSettings getArtifactsSettings() throws CannotConvertException;

  @NotNull
  String expandPath(@NotNull String path);
}
