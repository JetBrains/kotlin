// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppStaticLibrary;

public class CppStaticLibraryImpl extends CppBinaryImpl implements CppStaticLibrary {
  public CppStaticLibraryImpl(String name, String baseName, String variantName) {
    super(name, baseName, variantName);
  }

  public CppStaticLibraryImpl(CppStaticLibrary binary) {
    super(binary);
  }
}
