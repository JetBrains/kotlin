// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.lang.JavaVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class JavaHomeFinder {

  /**
   * Tries to find existing Java SDKs on this computer.
   * If no JDK found, returns possible folders to start file chooser.
   * @return suggested sdk home paths (sorted)
   */
  @NotNull
  public static List<String> suggestHomePaths() {
    return suggestHomePaths(false);
  }

  /**
   * Do the same as {@link #suggestHomePaths()} but always considers the embedded JRE,
   * for using in tests that are performed when the registry is not properly initialized
   * or that need the embedded JetBrains Runtime.
   */
  @NotNull
  public static List<String> suggestHomePaths(boolean forceEmbeddedJava) {
    JavaHomeFinderBasic javaFinder = getFinder(forceEmbeddedJava);
    if (javaFinder == null) return Collections.emptyList();

    Collection<String> foundPaths = javaFinder.findExistingJdks();
    ArrayList<String> paths = new ArrayList<>(foundPaths);
    paths.sort((o1, o2) -> Comparing.compare(JavaVersion.tryParse(o2), JavaVersion.tryParse(o1)));
    return paths;
  }

  private static boolean isDetectorEnabled(boolean forceEmbeddedJava) {
    return forceEmbeddedJava || Registry.is("java.detector.enabled", true);
  }

  private static JavaHomeFinderBasic getFinder(boolean forceEmbeddedJava) {
    if (!isDetectorEnabled(forceEmbeddedJava)) return null;

    if (SystemInfo.isWindows) {
      return new JavaHomeFinderWindows(forceEmbeddedJava);
    }
    if (SystemInfo.isMac) {
      return new JavaHomeFinderMac(forceEmbeddedJava);
    }
    if (SystemInfo.isLinux) {
      return new JavaHomeFinderBasic(forceEmbeddedJava, "/usr/java", "/opt/java", "/usr/lib/jvm");
    }
    if (SystemInfo.isSolaris) {
      return new JavaHomeFinderBasic(forceEmbeddedJava, "/usr/jdk");
    }

    return new JavaHomeFinderBasic(forceEmbeddedJava);
  }

  @Nullable
  public static String defaultJavaLocation() {
    if (SystemInfo.isWindows) {
      return JavaHomeFinderWindows.defaultJavaLocation;
    }
    if (SystemInfo.isMac) {
      return JavaHomeFinderMac.defaultJavaLocation;
    }

    if (SystemInfo.isLinux) {
      return "/opt/java";
    }

    if (SystemInfo.isSolaris) {
      return "/usr/jdk";
    }

    return null;
  }
}
