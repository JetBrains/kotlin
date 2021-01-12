// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.GradleSettings
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
                             " finished")

    var expectedExecutionTree: String
    when {
      isPerTaskOutputSupported -> expectedExecutionTree =
        "-\n" +
        " -successful\n" +
        "  :api:compileJava\n" +
        "  :api:processResources\n" +
        "  :api:classes\n" +
        "  :api:jar\n" +
        "  -:impl:compileJava\n" +
        "   -App.java\n" +
        "    uses or overrides a deprecated API.\n" +
        "  :impl:processResources\n" +
        "  :impl:classes"
      else -> expectedExecutionTree =
        "-\n" +
        " -successful\n" +
        "  :api:compileJava\n" +
        "  :api:processResources\n" +
        "  :api:classes\n" +
        "  :api:jar\n" +
        "  :impl:compileJava\n" +
        "  :impl:processResources\n" +
        "  -App.java\n" +
        "   uses or overrides a deprecated API.\n" +
        "  :impl:classes"
    }
    compileModules("project.impl.main")
    assertBuildViewTreeSame(expectedExecutionTree)

    when {
      isPerTaskOutputSupported -> expectedExecutionTree =
        "-\n" +
        " -failed\n" +
        "  -:brokenProject:compileJava\n" +
        "   -App2.java\n" +
        "    ';' expected\n" +
        "    invalid method declaration; return type required"
      else -> expectedExecutionTree =
        "-\n" +
        " -failed\n" +
        "  :brokenProject:compileJava\n" +
        "  -App2.java\n" +
        "   ';' expected\n" +
        "   invalid method declaration; return type required"
    }
    compileModules("project.brokenProject.main")
    assertBuildViewTreeSame(expectedExecutionTree)
  }

  @Test
  fun `test unresolved dependencies errors on Build`() {
    createProjectSubFile("src/main/java/my/pack/App.java",
                         "package my.pack;\n" +
                         "public class App {\n" +
                         "  public int method() { return 1; }\n" +
                         "}")
    createProjectSubFile("src/test/java/my/pack/AppTest.java",
                         "package my.pack;\n" +
                         "public class AppTest {\n" +
                         "  public void testMethod() { }\n" +
                         "}")
    val buildScript = GradleBuildScriptBuilderEx().withJavaPlugin()

    // get successfully imported project
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " finished")
    compileModules("project.test")
    assertBuildViewTreeEquals("-\n" +
                              " -successful\n" +
                              "  :compileJava\n" +
                              "  :processResources\n" +
                              "  :classes\n" +
                              "  :compileTestJava\n" +
                              "  :processTestResources\n" +
                              "  :testClasses")

    // check unresolved dependency w/o repositories
    buildScript.addDependency("testCompile 'junit:junit:4.12'")
    createProjectConfig(buildScript.generate())
    compileModules("project.test")

    val testCompileConfiguration = if (currentGradleVersion < GradleVersion.version("2.12")) "testCompile" else "testCompileClasspath"
    val requiredByProject = if (currentGradleVersion < GradleVersion.version("3.1")) ":project:unspecified" else "project :"
    val files = if (currentGradleVersion < GradleVersion.version("4.0")) "dependencies" else "files"
    val commonTreePart = "-\n" +
                         " -failed\n" +
                         "  :compileJava\n" +
                         "  :processResources\n" +
                         "  :classes\n"

    val usePerTaskError by lazy { currentGradleVersion >= GradleVersion.version("5.0") }
    assertBuildViewTreeEquals(commonTreePart +
                              if (usePerTaskError)
                                "  -:compileTestJava\n" +
                                "   Could not resolve junit:junit:4.12 because no repositories are defined"
                              else
                                "  :compileTestJava\n" +
                                "  Could not resolve junit:junit:4.12 because no repositories are defined")
    assertBuildViewSelectedNode("Could not resolve junit:junit:4.12 because no repositories are defined",
                                if (usePerTaskError)
                                  "Execution failed for task ':compileTestJava'.\n" +
                                  "> Could not resolve all files for configuration ':testCompileClasspath'.\n" +
                                  "   > Cannot resolve external dependency junit:junit:4.12 because no repositories are defined.\n" +
                                  "     Required by:\n" +
                                  "         project :\n" +
                                  "\n" +
                                  "Possible solution:\n" +
                                  " - Declare repository providing the artifact, see the documentation at https://docs.gradle.org/current/userguide/declaring_repositories.html\n" +
                                  "\n"
                                else
                                  "Could not resolve all $files for configuration ':$testCompileConfiguration'.\n" +
                                  "> Cannot resolve external dependency junit:junit:4.12 because no repositories are defined.\n" +
                                  "  Required by:\n" +
                                  "      $requiredByProject\n" +
                                  "\n" +
                                  "Possible solution:\n" +
                                  " - Declare repository providing the artifact, see the documentation at https://docs.gradle.org/current/userguide/declaring_repositories.html\n" +
                                  "\n")

    // check unresolved dependency for offline mode
    GradleSettings.getInstance(myProject).isOfflineWork = true
    buildScript.withMavenCentral(isGradleNewerOrSameThen("6.0"))
    buildScript.addDependency("testCompile 'junit:junit:99.99'")
    createProjectConfig(buildScript.generate())
    compileModules("project.test")

    assertBuildViewTreeEquals(commonTreePart +
                              if (usePerTaskError)
                                "  -:compileTestJava\n" +
                                "   Could not resolve junit:junit:99.99"
                              else
                                "  :compileTestJava\n" +
                                "  Could not resolve junit:junit:99.99")
    assertBuildViewSelectedNode("Could not resolve junit:junit:99.99",
                                if (usePerTaskError)
                                  "Execution failed for task ':compileTestJava'.\n" +
                                  "> Could not resolve all files for configuration ':testCompileClasspath'.\n" +
                                  "   > Could not resolve junit:junit:99.99.\n" +
                                  "     Required by:\n" +
                                  "         project :\n" +
                                  "      > No cached version of junit:junit:99.99 available for offline mode.\n" +
                                  "   > Could not resolve junit:junit:99.99.\n" +
                                  "     Required by:\n" +
                                  "         project :\n" +
                                  "      > No cached version of junit:junit:99.99 available for offline mode.\n" +
                                  "\n" +
                                  "Possible solution:\n" +
                                  " - Disable offline mode and rerun the build\n" +
                                  "\n"
                                else
                                  "Could not resolve all $files for configuration ':$testCompileConfiguration'.\n" +
                                  "> Could not resolve junit:junit:99.99.\n" +
                                  "  Required by:\n" +
                                  "      $requiredByProject\n" +
                                  "   > No cached version of junit:junit:99.99 available for offline mode.\n" +
                                  "> Could not resolve junit:junit:99.99.\n" +
                                  "  Required by:\n" +
                                  "      $requiredByProject\n" +
                                  "   > No cached version of junit:junit:99.99 available for offline mode.\n" +
                                  "\n" +
                                  "Possible solution:\n" +
                                  " - Disable offline mode and rerun the build\n" +
                                  "\n")

    // check unresolved dependency for disabled offline mode
    GradleSettings.getInstance(myProject).isOfflineWork = false
    compileModules("project.test")
    assertBuildViewTreeEquals(commonTreePart +
                              if (usePerTaskError)
                                "  -:compileTestJava\n" +
                                "   Could not resolve junit:junit:99.99"
                              else
                                "  :compileTestJava\n" +
                                "  Could not resolve junit:junit:99.99")
    assertBuildViewSelectedNode("Could not resolve junit:junit:99.99",
                                if (usePerTaskError)
                                  "Execution failed for task ':compileTestJava'.\n" +
                                  "> Could not resolve all files for configuration ':testCompileClasspath'.\n" +
                                  "   > Could not find junit:junit:99.99.\n" +
                                  "     Searched in the following locations:\n" +
                                  "       - https://repo.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.pom\n" +
                                  "       - https://repo.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.jar\n" +
                                  "     Required by:\n" +
                                  "         project :\n" +
                                  "   > Could not find junit:junit:99.99.\n" +
                                  "     Searched in the following locations:\n" +
                                  "       - https://repo.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.pom\n" +
                                  "       - https://repo.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.jar\n" +
                                  "     Required by:\n" +
                                  "         project :\n" +
                                  "\n" +
                                  "Possible solution:\n" +
                                  " - Declare repository providing the artifact, see the documentation at https://docs.gradle.org/current/userguide/declaring_repositories.html\n" +
                                  "\n"
                                else
                                  "Could not resolve all $files for configuration ':$testCompileConfiguration'.\n" +
                                  "> Could not find junit:junit:99.99.\n" +
                                  "  Searched in the following locations:\n" +
                                  "    $itemLinePrefix https://repo.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.pom\n" +
                                  "    $itemLinePrefix https://repo.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.jar\n" +
                                  "  Required by:\n" +
                                  "      $requiredByProject\n" +
                                  "> Could not find junit:junit:99.99.\n" +
                                  "  Searched in the following locations:\n" +
                                  "    $itemLinePrefix https://repo.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.pom\n" +
                                  "    $itemLinePrefix https://repo.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.jar\n" +
                                  "  Required by:\n" +
                                  "      $requiredByProject\n" +
                                  "\n" +
                                  "Possible solution:\n" +
                                  " - Declare repository providing the artifact, see the documentation at https://docs.gradle.org/current/userguide/declaring_repositories.html\n" +
                                  "\n")
  }
}
