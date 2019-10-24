// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

class GradleToolWindowOldGroupingTest extends GradleToolWindowTest {
  @Override
  void setUp() throws Exception {
    super.setUp()
    currentExternalProjectSettings.useQualifiedModuleNames = false
  }

  @Override
  protected String getPath() {
    def testDataPath = super.getPath()
    String testDataForOldGrouping = testDataPath + ".old"
    if (new File(testDataForOldGrouping).exists()) {
      return testDataForOldGrouping;
    } else {
      return testDataPath
    }
  }
}
