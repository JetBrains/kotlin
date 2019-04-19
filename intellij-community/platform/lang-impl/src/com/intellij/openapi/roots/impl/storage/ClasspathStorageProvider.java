// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl.storage;

import com.intellij.configurationStore.SaveSessionProducer;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * @author Vladislav.Kaznacheev
 */
public interface ClasspathStorageProvider {
  @NonNls ExtensionPointName<ClasspathStorageProvider> EXTENSION_POINT_NAME =
    new ExtensionPointName<>("com.intellij.classpathStorageProvider");

  @NonNls
  @NotNull
  String getID();

  @Nls
  @NotNull
  String getDescription();

  void assertCompatible(@NotNull ModuleRootModel model) throws ConfigurationException;

  void detach(@NotNull Module module);

  default void moduleRenamed(@NotNull Module module, @NotNull String oldName, @NotNull String newName) {
  }

  @NotNull
  ClasspathConverter createConverter(@NotNull Module module);

  @Nullable
  String getContentRoot(@NotNull ModuleRootModel model);

  default void modulePathChanged(@NotNull Module module) {
  }

  interface ClasspathConverter {
    @NotNull
    List<String> getFilePaths();

    @Nullable
    default SaveSessionProducer startExternalization() {
      return null;
    }

    void readClasspath(@NotNull ModifiableRootModel model) throws IOException;
  }
}
