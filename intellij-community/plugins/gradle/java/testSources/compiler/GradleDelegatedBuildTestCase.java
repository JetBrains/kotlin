// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.compiler;

import org.junit.Before;

/**
 * @author Vladislav.Soroka
 */
public abstract class GradleDelegatedBuildTestCase extends GradleCompilingTestCase {

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    getCurrentExternalProjectSettings().setDelegatedBuild(true);
    useProjectTaskManager = true;
  }
}
