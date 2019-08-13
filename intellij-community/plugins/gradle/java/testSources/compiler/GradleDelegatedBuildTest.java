// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.compiler;

import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.PathsList;
import org.junit.Test;

import java.util.Collections;

import static com.intellij.util.PathUtil.toSystemDependentName;

public class GradleDelegatedBuildTest extends GradleDelegatedBuildTestCase {
  @Test
  public void testDependentModulesOutputRefresh() throws Exception {
    createSettingsFile("include 'api', 'impl' ");

    createProjectSubFile("src/main/resources/dir/file.properties");
    createProjectSubFile("src/test/resources/dir/file-test.properties");

    createProjectSubFile("api/src/main/resources/dir/file-api.properties");
    createProjectSubFile("api/src/test/resources/dir/file-api-test.properties");

    createProjectSubFile("impl/src/main/resources/dir/file-impl.properties");
    createProjectSubFile("impl/src/test/resources/dir/file-impl-test.properties");
    importProject(
      "allprojects {\n" +
      "  apply plugin: 'java'\n" +
      "}\n" +
      "\n" +
      "dependencies {\n" +
      "  compile project(':api')\n" +
      "}\n" +
      "configure(project(':api')) {\n" +
      "  dependencies {\n" +
      "    compile project(':impl')\n" +
      "  }\n" +
      "}"
    );
    assertModules("project", "project.main", "project.test",
                  "project.api", "project.api.main", "project.api.test",
                  "project.impl", "project.impl.main", "project.impl.test");


    EdtTestUtil.runInEdtAndWait(() -> VirtualFileManager.getInstance().syncRefresh());
    PathsList pathsBeforeMake = new PathsList();
    OrderEnumerator.orderEntries(getModule("project.main")).withoutSdk().recursively().runtimeOnly().classes()
      .collectPaths(pathsBeforeMake);
    assertSameElements(pathsBeforeMake.getPathList(), Collections.emptyList());

    compileModules("project.main");
    PathsList pathsAfterMake = new PathsList();
    OrderEnumerator.orderEntries(getModule("project.main")).withoutSdk().recursively().runtimeOnly().classes().collectPaths(pathsAfterMake);
    assertSameElements(pathsAfterMake.getPathList(),
                       toSystemDependentName(path("build/resources/main")),
                       toSystemDependentName(path("api/build/resources/main")),
                       toSystemDependentName(path("impl/build/resources/main")));

    assertCopied("build/resources/main/dir/file.properties");
    assertNotCopied("build/resources/test/dir/file-test.properties");

    assertCopied("api/build/resources/main/dir/file-api.properties");
    assertNotCopied("api/build/resources/test/dir/file-api-test.properties");

    assertCopied("impl/build/resources/main/dir/file-impl.properties");
    assertNotCopied("impl/build/resources/test/dir/file-impl-test.properties");
  }
}
