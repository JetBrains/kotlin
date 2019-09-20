// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.compiler;

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase;

import java.io.File;

/**
 * @author Vladislav.Soroka
 */
public abstract class GradleCompilingTestCase extends GradleImportingTestCase {

  protected void assertCopied(String path) {
    assertTrue(file(path).exists());
  }

  protected void assertCopied(String path, String content) {
    assertCopied(path);
    assertSameLinesWithFile(path(path), content);
  }

  protected void assertNotCopied(String path) {
    assertFalse(file(path).exists());
  }

  @Override
  protected void assertArtifactOutputPath(String artifactName, String expected) {
    final String basePath = getArtifactBaseOutputPath(myProject);
    super.assertArtifactOutputPath(artifactName, basePath + expected);
  }

  protected void assertArtifactOutputPath(Module module, String artifactName, String expected) {
    final String basePath = getArtifactBaseOutputPath(module);
    super.assertArtifactOutputPath(artifactName, basePath + expected);
  }

  protected void assertArtifactOutputFile(String artifactName, String path, String content) {
    final String basePath = getArtifactBaseOutputPath(myProject);
    assertSameLinesWithFile(basePath + path, content);
  }

  protected void assertArtifactOutputFile(String artifactName, String path) {
    final String basePath = getArtifactBaseOutputPath(myProject);
    assertExists(new File(basePath + path));
  }

  private static String getArtifactBaseOutputPath(Project project) {
    return project.getBasePath() + "/out/artifacts";
  }

  private static String getArtifactBaseOutputPath(Module module) {
    String outputUrl = ExternalSystemApiUtil.getExternalProjectPath(module) + "/build/libs";
    return FileUtil.toSystemIndependentName(VfsUtilCore.urlToPath(outputUrl));
  }
}
