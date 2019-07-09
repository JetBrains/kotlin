// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.jetbrains.plugins.gradle.GradleDirectoryCompletionContributor;
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class GradleDirectoryCompletionContributorTest extends GradleImportingTestCase {
  @Test
  public void testVariants() throws Throwable {
    createProjectSubFile("settings.gradle", "include('submodule')");
    VirtualFile submoduleBuild = createProjectSubFile("submodule/build.gradle", "apply plugin:'java'");

    importProject(
      "apply plugin: 'java'\n" +
      "sourceSets { main { java { srcDirs 'src/main/java2' } } }");

    check(myProjectRoot,
          Pair.create("src/main/java", JavaSourceRootType.SOURCE),
          Pair.create("src/main/resources", JavaResourceRootType.RESOURCE),
          Pair.create("src/main/java2", JavaSourceRootType.SOURCE),
          Pair.create("src/test/java", JavaSourceRootType.TEST_SOURCE),
          Pair.create("src/test/resources", JavaResourceRootType.TEST_RESOURCE));

    check(submoduleBuild.getParent(),
          Pair.create("src/main/java", JavaSourceRootType.SOURCE),
          Pair.create("src/main/resources", JavaResourceRootType.RESOURCE),
          Pair.create("src/test/java", JavaSourceRootType.TEST_SOURCE),
          Pair.create("src/test/resources", JavaResourceRootType.TEST_RESOURCE));
  }

  private void check(VirtualFile dir, Pair<String, JpsModuleSourceRootType<?>>... expected) {
    PsiDirectory psiDir = ReadAction.compute(() -> PsiManager.getInstance(myProject).findDirectory(dir));

    List<Pair<String, JpsModuleSourceRootType<?>>> map = ContainerUtil.map(
      new GradleDirectoryCompletionContributor().getVariants(psiDir),
      it -> Pair.create(FileUtil.getRelativePath(dir.getPath(), FileUtil.toSystemIndependentName(it.getPath()), '/'), it.getRootType()));

    assertSameElements(map, expected);
  }


  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{{BASE_GRADLE_VERSION}});
  }
}