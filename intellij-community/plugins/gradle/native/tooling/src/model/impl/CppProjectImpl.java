// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppComponent;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppProject;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppTestSuite;

/**
 * @author Vladislav.Soroka
 */
public final class CppProjectImpl implements CppProject {
  @Nullable
  private CppComponent mainComponent;
  @Nullable
  private CppTestSuite testComponent;

  public CppProjectImpl() {
  }

  public CppProjectImpl(CppProject cppProject) {
    CppComponent mainComponent = cppProject.getMainComponent();
    if (mainComponent != null) {
      this.mainComponent = new CppComponentImpl(mainComponent);
    }
    CppTestSuite testComponent = cppProject.getTestComponent();
    if (testComponent != null) {
      this.testComponent = new CppTestSuiteImpl(testComponent);
    }
  }

  @Nullable
  @Override
  public CppComponent getMainComponent() {
    return mainComponent;
  }

  public void setMainComponent(@Nullable CppComponent mainComponent) {
    this.mainComponent = mainComponent;
  }

  @Nullable
  @Override
  public CppTestSuite getTestComponent() {
    return testComponent;
  }

  public void setTestComponent(@Nullable CppTestSuite testComponent) {
    this.testComponent = testComponent;
  }
}
