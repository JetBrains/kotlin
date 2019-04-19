// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppTestSuite;

public class CppTestSuiteImpl extends CppComponentImpl implements CppTestSuite {
  public CppTestSuiteImpl(String name, String baseName) {
    super(name, baseName);
  }

  public CppTestSuiteImpl(CppTestSuite component) {
    super(component);
  }
}
