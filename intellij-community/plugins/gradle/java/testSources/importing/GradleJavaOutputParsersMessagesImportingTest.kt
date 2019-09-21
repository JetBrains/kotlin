// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import org.gradle.util.GradleVersion
import org.junit.Test

@Suppress("GrUnresolvedAccess")
class GradleJavaOutputParsersMessagesImportingTest : GradleOutputParsersMessagesImportingTest() {

  @Test
  fun `test build script errors on Build`() {
    createSettingsFile("include 'api', 'impl', 'brokenProject' ")
    createProjectSubFile("impl/build.gradle",
                         "dependencies {\n" +
                         "   compile project(':api')\n" +
                         "}")
    createProjectSubFile("api/src/main/java/my/pack/Api.java",
                         "package my.pack;\n" +
                         "public interface Api {\n" +
                         "  @Deprecated" +
                         "  public int method();" +
                         "}")
    createProjectSubFile("impl/src/main/java/my/pack/App.java",
                         "package my.pack;\n" +
                         "import my.pack.Api;\n" +
                         "public class App implements Api {\n" +
                         "  public int method() { return 1; }" +
                         "}")
    createProjectSubFile("brokenProject/src/main/java/my/pack/App2.java",
                         "package my.pack;\n" +
                         "import my.pack.Api;\n" +
                         "public class App2 {\n" +
                         "  public int metho d() { return 1; }" +
                         "}")


    importProject("subprojects { apply plugin: 'java' }")
    assertSyncViewTreeEquals("-\n" +
                             " successful")

    var expectedExecutionTree: String
    val isPerTaskOutputSupported = currentGradleVersion < GradleVersion.version("4.7")
    when {
      isPerTaskOutputSupported -> expectedExecutionTree =
        "-\n" +
        " -successful\n" +
        "  :api:compileJava\n" +
        "  :api:processResources\n" +
        "  :api:classes\n" +
        "  :api:jar\n" +
        "  :impl:compileJava\n" +
        "  :impl:processResources\n" +
        "  -impl/src/main/java/my/pack/App.java\n" +
        "   uses or overrides a deprecated API.\n" +
        "  :impl:classes"
      else -> expectedExecutionTree =
        "-\n" +
        " -successful\n" +
        "  :api:compileJava\n" +
        "  :api:processResources\n" +
        "  :api:classes\n" +
        "  :api:jar\n" +
        "  -:impl:compileJava\n" +
        "   -impl/src/main/java/my/pack/App.java\n" +
        "    uses or overrides a deprecated API.\n" +
        "  :impl:processResources\n" +
        "  :impl:classes"
    }
    compileModules("project.impl.main")
    assertBuildViewTreeSame(expectedExecutionTree)

    when {
      isPerTaskOutputSupported -> expectedExecutionTree =
        "-\n" +
        " -failed\n" +
        "  :brokenProject:compileJava\n" +
        "  -brokenProject/src/main/java/my/pack/App2.java\n" +
        "   ';' expected\n" +
        "   invalid method declaration; return type required"
      else -> expectedExecutionTree =
        "-\n" +
        " -failed\n" +
        "  -:brokenProject:compileJava\n" +
        "   -brokenProject/src/main/java/my/pack/App2.java\n" +
        "    ';' expected\n" +
        "    invalid method declaration; return type required"
    }
    compileModules("project.brokenProject.main")
    assertBuildViewTreeSame(expectedExecutionTree)
  }
}
