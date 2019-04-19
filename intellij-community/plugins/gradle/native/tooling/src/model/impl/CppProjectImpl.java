// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppComponent;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppProject;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppTestSuite;

/**
 * @author Vladislav.Soroka
 */
public class CppProjectImpl implements CppProject {

  @Nullable
  private CppComponent myMainComponent;
  @Nullable
  private CppTestSuite myTestComponent;

  public CppProjectImpl() {
  }

  public CppProjectImpl(CppProject cppProject) {
    CppComponent mainComponent = cppProject.getMainComponent();
    if (mainComponent != null) {
      myMainComponent = new CppComponentImpl(mainComponent);
    }
    CppTestSuite testComponent = cppProject.getTestComponent();
    if (testComponent != null) {
      myTestComponent = new CppTestSuiteImpl(testComponent);
    }
  }

  @Nullable
  @Override
  public CppComponent getMainComponent() {
    return myMainComponent;
  }

  public void setMainComponent(@Nullable CppComponent mainComponent) {
    myMainComponent = mainComponent;
  }

  @Nullable
  @Override
  public CppTestSuite getTestComponent() {
    return myTestComponent;
  }

  public void setTestComponent(@Nullable CppTestSuite testComponent) {
    myTestComponent = testComponent;
  }
}
