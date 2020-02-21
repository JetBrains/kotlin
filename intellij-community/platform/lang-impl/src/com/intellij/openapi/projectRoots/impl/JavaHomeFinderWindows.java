// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

class JavaHomeFinderWindows extends JavaHomeFinderBase {

  @Override
  public @NotNull Set<String> findExistingJdksImpl() {
    Set<String> result = new TreeSet<>();
    Set<File> roots = findRootsToScan();
    for (File root : roots) {
       scanFolder(root, true, result);
    }
    return result;
  }

  @NotNull
  private static Set<File> findRootsToScan() {
    TreeSet<File> roots = new TreeSet<>();
    File javaHome = getJavaHome();
    if (javaHome != null) {
      roots.add(javaHome);
    }
    File[] fsRoots = File.listRoots();
    for (File root : fsRoots) {
      if (root.exists()) {
        File candidate = new File(new File(root, "Program Files"), "Java");
        if (candidate.isDirectory()) roots.add(candidate);
        candidate =  new File(new File(root, "Program Files (x86)"), "Java");
        if (candidate.isDirectory()) roots.add(candidate);
      }
    }
    return roots;
  }
}
