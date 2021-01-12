// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import com.intellij.serialization.PropertyMapping;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppBinary;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppTestSuite;

import java.util.Set;

public class CppTestSuiteImpl extends CppComponentImpl implements CppTestSuite {

  @PropertyMapping({"name", "baseName", "binaries"})
  private CppTestSuiteImpl(String name, String baseName, Set<? extends CppBinary> binaries) {
    super(name, baseName, binaries);
  }

  public CppTestSuiteImpl(String name, String baseName) {
    super(name, baseName);
  }

  public CppTestSuiteImpl(CppTestSuite component) {
    super(component);
  }
}
