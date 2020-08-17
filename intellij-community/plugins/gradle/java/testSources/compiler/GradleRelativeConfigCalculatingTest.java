// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.compiler;

import com.intellij.compiler.server.BuildManager;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.gradle.internal.impldep.org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.gradle.internal.impldep.org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.executeOnEdt;

/**
 * @author Aleksei.Cherepanov
 */
public class GradleRelativeConfigCalculatingTest extends GradleJpsCompilingTestCase {
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{{BASE_GRADLE_VERSION}});
  }

  @Test
  public void testGradleRelativeConfigEquality() throws Exception {
    setupAndBuildProject("first");
    setupAndBuildProject("second");
    assertConfigEquality();
  }

  private void setupAndBuildProject(String subfolderName) throws Exception {
    createProjectSubDir(subfolderName);
    VirtualFile projectDir = createProjectSubDir(subfolderName + "/projectName");
    createProjectSubFile(subfolderName + "/projectName/src/main/resources/dir/file.properties");
    createProjectSubFile(subfolderName + "/projectName/src/test/resources/dir/file-test.properties");

    createProjectSubFile(subfolderName + "/projectName/build.gradle", "apply plugin: 'java'");
    createProjectSubFile(subfolderName + "/projectName/settings.gradle", "");
    try {
      myProject = executeOnEdt(() -> ProjectUtil.openOrImport(projectDir.toNioPath()));

      assertModules("projectName", "projectName.main", "projectName.test");
      compileModules("projectName.main", "projectName.test");

      assertCopied(subfolderName + "/projectName/out/production/resources/dir/file.properties");
      assertCopied(subfolderName + "/projectName/out/test/resources/dir/file-test.properties");
    } finally {
      edt(() -> {
        if (myProject != null && !myProject.isDisposed()) {
          PlatformTestUtil.forceCloseProjectWithoutSaving(myProject);
        }
      });
    }
  }

  private static void assertConfigEquality() {
    BuildManager buildManager = BuildManager.getInstance();
    if (buildManager == null) fail("BuildManager is disposed");

    File buildSystemDirectory = new File(String.valueOf(buildManager.getBuildSystemDirectory()));
    if(!buildSystemDirectory.exists()) fail("compile-server folder does not exists");

    File[] dirs = buildSystemDirectory.listFiles();
    if(dirs.length != 2) fail("Number of project caches != 2");

    List<String> firstProjectConfig = getConfigsList(dirs[0]);
    List<String> secondProjectConfig = getConfigsList(dirs[1]);

    Collections.sort(firstProjectConfig);
    Collections.sort(secondProjectConfig);

    assertEquals(firstProjectConfig, secondProjectConfig);
  }

  private static List<String> getConfigsList(File targetsDir) {
    Collection<File> configFiles = FileUtils.listFiles(
      targetsDir,
      new RegexFileFilter("gradle-resources-(production|test)\\/.*config\\.dat"),
      DirectoryFileFilter.DIRECTORY
    );

    return ContainerUtil.map(configFiles, configFile -> {
      byte[] bytes = new byte[0];
      try {
        bytes = Files.readAllBytes(configFile.toPath());
      }
      catch (IOException e) {
        e.printStackTrace();
      }

      return new String(bytes, CharsetToolkit.UTF8_CHARSET);
    });
  }
}