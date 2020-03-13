// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl

import java.io.File
import java.util.*

internal class JavaHomeFinderWindows : JavaHomeFinderBasic {
  constructor(forceEmbeddedJava: Boolean) : super(forceEmbeddedJava) {
    registerFinder {
      val fsRoots = File.listRoots() ?: return@registerFinder emptySet<String>()
      val roots: MutableSet<File> = HashSet()
      for (root in fsRoots) {
        if (!root.exists()) continue
        roots.add(File(File(root, "Program Files"), "Java"))
        roots.add(File(File(root, "Program Files (x86)"), "Java"))
        roots.add(File(root, "Java"))
      }
      scanAll(roots, true)
    }
  }
}
