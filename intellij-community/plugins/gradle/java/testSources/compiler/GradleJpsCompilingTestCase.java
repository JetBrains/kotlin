// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.compiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.config.GradleResourceCompilerConfigurationGenerator;
import org.junit.Before;

/**
 * @author Vladislav.Soroka
 */
public abstract class GradleJpsCompilingTestCase extends GradleCompilingTestCase {

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    getCurrentExternalProjectSettings().setDelegatedBuild(false);
  }

  @Override
  protected void setUpInWriteAction() throws Exception {
    super.setUpInWriteAction();

    final GradleResourceCompilerConfigurationGenerator buildConfigurationGenerator = new GradleResourceCompilerConfigurationGenerator(myProject);
    CompilerManager.getInstance(myProject).addBeforeTask(new CompileTask() {
      @Override
      public boolean execute(@NotNull CompileContext context) {
        ApplicationManager.getApplication().runReadAction(() -> buildConfigurationGenerator.generateBuildConfiguration(context));
        return true;
      }
    });
  }
}
