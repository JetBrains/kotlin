// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.plugins.gradle.service.project.GradleAutoImportAware;
import org.junit.Test;

public class GradleAutoImportAwareTest extends GradleImportingTestCase {

  @Test
  public void testCompilerOutputNotWatched() throws Exception {
    createProjectSubFile("src/main/java/my/pack/gradle/A.java");
    final VirtualFile file = createProjectSubFile("buildScripts/myScript.gradle");
    importProjectUsingSingeModulePerGradleProject("apply plugin: 'java'");

    assertModules("project");

    final GradleAutoImportAware gradleAutoImportAware = new GradleAutoImportAware();

    final Module module = ModuleManager.getInstance(myProject).getModules()[0];
    final CompilerModuleExtension compilerModuleExtension = CompilerModuleExtension.getInstance(module);
    final String compilerOutputDir = VfsUtilCore.urlToPath(compilerModuleExtension.getCompilerOutputUrl() + "/my/pack/gradle");
    final String testDataOutputDir = VfsUtilCore.urlToPath(compilerModuleExtension.getCompilerOutputUrlForTests() + "/testData.gradle");

    assertNull(gradleAutoImportAware.getAffectedExternalProjectPath(compilerOutputDir, myProject));
    assertNull(gradleAutoImportAware.getAffectedExternalProjectPath(testDataOutputDir, myProject));

    assertNotNull(gradleAutoImportAware.getAffectedExternalProjectPath(file.getPath(), myProject));
  }
}
