/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.webcore.packaging;

import com.intellij.util.CatchingConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Sergey Simonchik
 */
public abstract class PackageManagementServiceEx extends PackageManagementService {

  public abstract void updatePackage(@NotNull InstalledPackage installedPackage,
                                     @Nullable String version,
                                     @NotNull Listener listener);

  public boolean shouldFetchLatestVersionsForOnlyInstalledPackages() {
    return true;
  }

  public abstract void fetchLatestVersion(@NotNull InstalledPackage pkg, @NotNull final CatchingConsumer<String, Exception> consumer);

  public void installPackage(final RepoPackage repoPackage,
                             @Nullable final String version,
                             @Nullable String extraOptions,
                             final Listener listener,
                             @NotNull final File workingDir) {}
}
