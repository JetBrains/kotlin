// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkInstaller;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class JavaHomeFinderBasic {
  private final Logger LOG = Logger.getInstance(getClass());
  private final List<Supplier<Set<String>>> myFinders = new ArrayList<>();

  JavaHomeFinderBasic(boolean forceEmbeddedJava, String... paths) {
    myFinders.add(this::checkDefaultLocations);
    myFinders.add(this::findInPATH);
    myFinders.add(() -> scanAll(
      Stream.of(paths).map(it -> new File(it)).collect(Collectors.toList()),
      true)
    );

    if (forceEmbeddedJava || Registry.is("java.detector.include.embedded", false)) {
      myFinders.add(() -> scanAll(getJavaHome(), false));
    }
  }

  protected void registerFinder(@NotNull Supplier<Set<String>> finder) {
    myFinders.add(finder);
  }

  @NotNull
  public final Set<String> findExistingJdks() {
    Set<String> result = new TreeSet<>();

    for (Supplier<Set<String>> action : myFinders) {
      try {
        result.addAll(action.get());
      }
      catch (Exception e) {
        LOG.warn("Failed to find Java Home. " + e.getMessage(), e);
      }
    }

    return result;
  }

  @NotNull
  private Set<String> findInPATH() {
    try {
      String pathVarString = EnvironmentUtil.getValue("PATH");
      if (pathVarString == null || pathVarString.isEmpty()) return Collections.emptySet();

      Set<File> dirsToCheck = new HashSet<>();
      for (String p : pathVarString.split(File.pathSeparator)) {
        File dir = new File(p);
        if (!StringUtilRt.equal(dir.getName(), "bin", SystemInfo.isFileSystemCaseSensitive)) continue;

        File parentFile = dir.getParentFile();
        if (parentFile == null) continue;

        dirsToCheck.addAll(listPossibleJdkInstallRootsFromHomes(parentFile));
      }

      return scanAll(dirsToCheck, false);
    }
    catch (Exception e) {
      LOG.warn("Failed to scan PATH for JDKs. " + e.getMessage(), e);
      return Collections.emptySet();
    }
  }

  @NotNull
  private Set<String> checkDefaultLocations() {
    if (ApplicationManager.getApplication() == null) return Collections.emptySet();

    Set<File> paths = new HashSet<>();
    paths.add(JdkInstaller.getInstance().defaultInstallDir());

    for (Sdk jdk : ProjectJdkTable.getInstance().getAllJdks()) {
      if (!(jdk.getSdkType() instanceof JavaSdkType) || jdk.getSdkType() instanceof DependentSdkType) continue;

      String homePath = jdk.getHomePath();
      if (homePath == null) continue;

      paths.addAll(listPossibleJdkInstallRootsFromHomes(new File(homePath)));
    }

    return scanAll(paths, true);
  }

  @NotNull
  protected Set<String> scanAll(@Nullable File file, boolean includeNestDirs) {
    if (file == null) return Collections.emptySet();
    return scanAll(Collections.singleton(file), includeNestDirs);
  }

  @NotNull
  protected Set<String> scanAll(@NotNull Collection<File> files, boolean includeNestDirs) {
    Set<String> result = new HashSet<>();
    for (File root : new HashSet<>(files)) {
      scanFolder(root, includeNestDirs, result);
    }
    return result;
  }

  private void scanFolder(@NotNull File folder, boolean includeNestDirs, @NotNull Collection<? super String> result) {
    if (JdkUtil.checkForJdk(folder)) {
      result.add(folder.getAbsolutePath());
      return;
    }

    if (!includeNestDirs) return;
    File[] files = folder.listFiles();
    if (files == null) return;

    for (File candidate : files) {
      for (File adjusted : listPossibleJdkHomesFromInstallRoot(candidate)) {
        scanFolder(adjusted, false, result);
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

  @Nullable
  protected static File getJavaHome() {
    String property = SystemProperties.getJavaHome();
    if (property == null) return null;

    File javaHome = new File(property).getParentFile();//actually java.home points to to jre home
    return javaHome == null || !javaHome.isDirectory() ? null : javaHome;
  }
}
