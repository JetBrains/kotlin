// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkInstaller;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

abstract class JavaHomeFinderBase {
  private final Logger LOG = Logger.getInstance(getClass());

  @NotNull
  protected abstract Set<String> findExistingJdksImpl();

  @NotNull
  public final Set<String> findExistingJdks() {
    Set<String> result = new TreeSet<>();
    result.addAll(checkDefaultLocations());
    result.addAll(findExistingJdksImpl());
    return result;
  }

  @NotNull
  private Set<String> checkDefaultLocations() {
    if (ApplicationManager.getApplication() == null) return Collections.emptySet();

    try {
      Set<File> paths = new HashSet<>();
      paths.add(new File(JdkInstaller.getInstance().defaultInstallDir()));

      for (Sdk jdk : ProjectJdkTable.getInstance().getAllJdks()) {
        if (!(jdk.getSdkType() instanceof JavaSdkType) || jdk.getSdkType() instanceof DependentSdkType) continue;

        String homePath = jdk.getHomePath();
        if (homePath == null) continue;

        paths.addAll(listPossibleJdkInstallRootsFromHomes(new File(homePath)));
      }

      Set<String> result = new HashSet<>();
      for (File home : paths) {
        scanFolder(home, true, result);
      }
      return result;
    }
    catch (Exception e) {
      LOG.warn("Failed to scan for neighbour JDKs. " + e.getMessage(), e);
      return Collections.emptySet();
    }
  }

  protected void scanFolder(@NotNull File folder, boolean includeNestDirs, @NotNull Collection<? super String> result) {
    if (JdkUtil.checkForJdk(folder)) {
      result.add(folder.getAbsolutePath());
      return;
    }

    if (includeNestDirs) {
      for (File candidate : ObjectUtils.notNull(folder.listFiles(), ArrayUtilRt.EMPTY_FILE_ARRAY)) {
        for (File adjusted : listPossibleJdkHomesFromInstallRoot(candidate)) {
          scanFolder(adjusted, false, result);
        }
      }
    }
  }

  @NotNull
  protected List<File> listPossibleJdkHomesFromInstallRoot(@NotNull File file) {
    return Collections.singletonList(file);
  }

  @NotNull
  protected List<File> listPossibleJdkInstallRootsFromHomes(@NotNull File file) {
    return Collections.singletonList(file);
  }

  protected static File getJavaHome() {
    String property = SystemProperties.getJavaHome();
    if (property == null)
      return null;

    File javaHome = new File(property).getParentFile();//actually java.home points to to jre home
    return javaHome == null || !javaHome.isDirectory() ? null : javaHome;
  }
}
