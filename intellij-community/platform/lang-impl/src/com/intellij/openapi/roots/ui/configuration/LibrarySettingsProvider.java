// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryKind;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides configurables for library settings for certain library type (platform-based products).
 * @author Rustam Vishnyakov
 */
public abstract class LibrarySettingsProvider {
  public static final ExtensionPointName<LibrarySettingsProvider> EP_NAME =
    ExtensionPointName.create("com.intellij.librarySettingsProvider");

  @NotNull
  @Contract(pure = true)
  public abstract LibraryKind getLibraryKind();
  @Contract(pure = true)
  public abstract Configurable getAdditionalSettingsConfigurable(Project project);

  @Nullable
  @Contract(pure = true)
  public static Configurable getAdditionalSettingsConfigurable(Project project, LibraryKind libKind) {
    LibrarySettingsProvider provider = forLibraryType(libKind);
    if (provider == null) return null;
    return provider.getAdditionalSettingsConfigurable(project);
  }

  @Nullable
  @Contract(pure = true)
  public static LibrarySettingsProvider forLibraryType(LibraryKind libType) {
    for (LibrarySettingsProvider provider : EP_NAME.getExtensionList()) {
      if (provider.getLibraryKind().equals(libType)) {
        return provider;
      }
    }
    return null;
  }
}
