// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

class JavaHomeFinderWindows extends JavaHomeFinder {

  @NotNull
  @Override
  protected List<String> findExistingJdks() {
    ArrayList<String> result = new ArrayList<>();
    Set<File> roots = findRootsToScan();
    for (File root : roots) {
       scanFolder(root, true, result);
    }
    result.sort((o1, o2) -> {
      String name1 = new File(o1).getName();
      String name2 = new File(o2).getName();
      return StringUtil.compareVersionNumbers(name1, name2);
    });
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
