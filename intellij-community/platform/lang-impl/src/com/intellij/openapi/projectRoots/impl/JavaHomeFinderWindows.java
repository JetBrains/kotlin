// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class JavaHomeFinderWindows extends JavaHomeFinderBasic {
  JavaHomeFinderWindows(boolean forceEmbeddedJava) {
    super(forceEmbeddedJava);

    registerFinder(() -> {
      File[] fsRoots = File.listRoots();
      if (fsRoots == null) return Collections.emptySet();

      Set<File> roots = new HashSet<>();
      for (File root : fsRoots) {
        if (!root.exists()) continue;

        roots.add(new File(new File(root, "Program Files"), "Java"));
        roots.add(new File(new File(root, "Program Files (x86)"), "Java"));
        roots.add(new File(root, "Java"));
      }

      return scanAll(roots, true);
    });
  }
}
