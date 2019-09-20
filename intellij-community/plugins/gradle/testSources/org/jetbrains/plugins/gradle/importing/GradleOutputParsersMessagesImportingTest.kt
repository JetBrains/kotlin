// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.externalSystem.importing.ImportSpec
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.util.io.FileUtil
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.GradleSystemSettings
import org.junit.Test

@Suppress("GrUnresolvedAccess")
open class GradleOutputParsersMessagesImportingTest : BuildViewMessagesImportingTestCase() {
  val itemLinePrefix by lazy { if (currentGradleVersion < GradleVersion.version("4.8")) " " else "-" }
  val isPerTaskOutputSupported by lazy { currentGradleVersion >= GradleVersion.version("4.7") }
  private var enableStackTraceImportingOption = false

  // do not inject repository
  override fun injectRepo(config: String): String = config

  override fun createImportSpec(): ImportSpec {
    val baseImportSpec = super.createImportSpec()
    val baseArguments = baseImportSpec.arguments
    val importSpecBuilder = ImportSpecBuilder(baseImportSpec)
    if (enableStackTraceImportingOption) {
      if (baseArguments == null || !baseArguments.contains("--stacktrace")) {
        importSpecBuilder.withArguments("${baseArguments} --stacktrace")
      }
    }
    else {
      if (baseArguments != null) {
        importSpecBuilder.withArguments(baseArguments.replace("--stacktrace", ""))
      }
    }
    return importSpecBuilder.build()
  }

  @Test
  fun `test build script errors on Sync`() {
    createSettingsFile("include 'api', 'impl' ")
    createProjectSubFile("impl/build.gradle",
                         "dependencies {\n" +
                         "   ghostConf project(':api')\n" +
                         "}")
    importProject("subprojects { apply plugin: 'java' }")

    val expectedExecutionTree: String
    when {
      currentGradleVersion < GradleVersion.version("2.14") -> expectedExecutionTree =
        "-\n" +
        " -failed\n" +
        "  -impl/build.gradle\n" +
        "   Could not find method ghostConf() for arguments [project ':api'] on project ':impl'"
      else -> expectedExecutionTree =
        "-\n" +
        " -failed\n" +
        "  -impl/build.gradle\n" +
        "   Could not find method ghostConf() for arguments [project ':api'] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler"
    }
    assertSyncViewTreeEquals(expectedExecutionTree)
  }

  @Test
  fun `test build script plugins errors on Sync`() {
    createProjectSubFile("buildSrc/src/main/java/example/SomePlugin.java",
                         "package example;\n" +
                         "\n" +
                         "import org.gradle.api.Plugin;\n" +
                         "import org.gradle.api.Project;\n" +
                         "\n" +
                         "public class SomePlugin implements Plugin<Project> {\n" +
                         "    public void apply(Project project) {\n" +
                         "        throw new IllegalArgumentException(\"Something's wrong!\");\n" +
                         "    }\n" +
                         "}\n")
    importProject("apply plugin: example.SomePlugin")

    var expectedExecutionTree: String = "-\n" +
                                        " -failed\n"
    if (currentGradleVersion >= GradleVersion.version("3.3") &&
        currentGradleVersion < GradleVersion.version("4.5")) {
      expectedExecutionTree += "  :buildSrc:clean\n"
    }

    if (currentGradleVersion >= GradleVersion.version("3.3")) {
      expectedExecutionTree += "  :buildSrc:compileJava\n" +
                               "  :buildSrc:compileGroovy\n" +
                               "  :buildSrc:processResources\n" +
                               "  :buildSrc:classes\n" +
                               "  :buildSrc:jar\n" +
                               "  :buildSrc:assemble\n" +
                               "  :buildSrc:compileTestJava\n" +
                               "  :buildSrc:compileTestGroovy\n" +
                               "  :buildSrc:processTestResources\n" +
                               "  :buildSrc:testClasses\n" +
                               "  :buildSrc:test\n" +
                               "  :buildSrc:check\n" +
                               "  :buildSrc:build\n"
    }

    expectedExecutionTree += "  -build.gradle\n" +
                             "   Something's wrong!"
    assertSyncViewTreeEquals(expectedExecutionTree)

    val filePath = FileUtil.toSystemDependentName(myProjectConfig.path)
    assertSyncViewSelectedNode("Something's wrong!",
                               "Build file '$filePath' line: 1\n\n" +
                               "A problem occurred evaluating root project 'project'.\n" +
                               "> Failed to apply plugin [class 'example.SomePlugin']\n" +
                               "   > Something's wrong!\n")

  }

  @Test
  fun `test unresolved dependencies errors on Sync`() {
    val buildScript = GradleBuildScriptBuilderEx().withJavaPlugin()

    // check sunny case
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " finished")

    // check unresolved dependency w/o repositories
    buildScript.addDependency("testCompile 'junit:junit:4.12'")
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " -finished\n" +
                             "  Could not resolve junit:junit:4.12")
    assertSyncViewSelectedNode("Could not resolve junit:junit:4.12",
                               "Cannot resolve external dependency junit:junit:4.12 because no repositories are defined.\n" +
                               "\n" +
                               "Possible solution:\n" +
                               " - Declare repository providing the artifact, see the documentation at https://docs.gradle.org/current/userguide/declaring_repositories.html\n" +
                               "\n")

    // successful import when repository is added
    buildScript.withMavenCentral()
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " finished")

    // check unresolved dependency for offline mode
    GradleSystemSettings.getInstance().isOfflineWork = true
    buildScript.addDependency("testCompile 'junit:junit:99.99'")
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " -finished\n" +
                             "  Could not resolve junit:junit:99.99")
    assertSyncViewSelectedNode("Could not resolve junit:junit:99.99",
                               "Could not resolve junit:junit:99.99.\n" +
                               "\n" +
                               "Possible solution:\n" +
                               " - Disable offline mode and reimport the project\n" +
                               "\n")

    // check unresolved dependency for offline mode when merged project used
    GradleSystemSettings.getInstance().isOfflineWork = true
    currentExternalProjectSettings.isResolveModulePerSourceSet = false
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " -finished\n" +
                             "  Could not resolve junit:junit:99.99")
    assertSyncViewSelectedNode("Could not resolve junit:junit:99.99",
                               "Could not resolve junit:junit:99.99.\n" +
                               "\n" +
                               "Possible solution:\n" +
                               " - Disable offline mode and reimport the project\n" +
                               "\n")

    currentExternalProjectSettings.isResolveModulePerSourceSet = true
    // check unresolved dependency for disabled offline mode
    GradleSystemSettings.getInstance().isOfflineWork = false
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " -finished\n" +
                             "  Could not resolve junit:junit:99.99")
    assertSyncViewSelectedNode("Could not resolve junit:junit:99.99",
                               "Could not find junit:junit:99.99.\n" +
                               "Searched in the following locations:\n" +
                               "  $itemLinePrefix http://maven.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.pom\n" +
                               "  $itemLinePrefix http://maven.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.jar\n" +
                               "\n" +
                               "Possible solution:\n" +
                               " - Declare repository providing the artifact, see the documentation at https://docs.gradle.org/current/userguide/declaring_repositories.html\n" +
                               "\n")
  }

  @Test
  fun `test unresolved build script dependencies errors on Sync`() {
    val buildScript = GradleBuildScriptBuilderEx()
    val requiredByProject = if (currentGradleVersion < GradleVersion.version("3.1")) ":project:unspecified" else "project :"
    val artifacts = when {
      currentGradleVersion < GradleVersion.version("4.0") -> "dependencies"
      currentGradleVersion < GradleVersion.version("4.6") -> "files"
      else -> "artifacts"
    }

    // check unresolved dependency w/o repositories
    buildScript.addBuildScriptDependency("classpath 'junit:junit:4.12'")
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " -failed\n" +
                             "  Could not resolve junit:junit:4.12 because no repositories are defined")
    assertSyncViewSelectedNode("Could not resolve junit:junit:4.12 because no repositories are defined",
                               "A problem occurred configuring root project 'project'.\n" +
                               "> Could not resolve all $artifacts for configuration ':classpath'.\n" +
                               "   > Cannot resolve external dependency junit:junit:4.12 because no repositories are defined.\n" +
                               "     Required by:\n" +
                               "         $requiredByProject\n" +
                               "\n" +
                               "Possible solution:\n" +
                               " - Declare repository providing the artifact, see the documentation at https://docs.gradle.org/current/userguide/declaring_repositories.html\n" +
                               "\n")

    // successful import when repository is added
    buildScript.withBuildScriptMavenCentral()
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " finished")

    // check unresolved dependency for offline mode
    GradleSystemSettings.getInstance().isOfflineWork = true
    buildScript.addBuildScriptDependency("classpath 'junit:junit:99.99'")
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " -failed\n" +
                             "  Could not resolve junit:junit:99.99")
    assertSyncViewSelectedNode("Could not resolve junit:junit:99.99",
                               "A problem occurred configuring root project 'project'.\n" +
                               "> Could not resolve all $artifacts for configuration ':classpath'.\n" +
                               "   > Could not resolve junit:junit:99.99.\n" +
                               "     Required by:\n" +
                               "         $requiredByProject\n" +
                               "      > No cached version of junit:junit:99.99 available for offline mode.\n" +
                               "   > Could not resolve junit:junit:99.99.\n" +
                               "     Required by:\n" +
                               "         $requiredByProject\n" +
                               "      > No cached version of junit:junit:99.99 available for offline mode.\n" +
                               "\n" +
                               "Possible solution:\n" +
                               " - Disable offline mode and rerun the build\n" +
                               "\n")

    // check unresolved dependency for disabled offline mode
    GradleSystemSettings.getInstance().isOfflineWork = false
    importProject(buildScript.generate())
    assertSyncViewTreeEquals("-\n" +
                             " -failed\n" +
                             "  Could not resolve junit:junit:99.99")
    assertSyncViewSelectedNode("Could not resolve junit:junit:99.99",
                               "A problem occurred configuring root project 'project'.\n" +
                               "> Could not resolve all $artifacts for configuration ':classpath'.\n" +
                               "   > Could not find junit:junit:99.99.\n" +
                               "     Searched in the following locations:\n" +
                               "       $itemLinePrefix http://maven.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.pom\n" +
                               "       $itemLinePrefix http://maven.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.jar\n" +
                               "     Required by:\n" +
                               "         $requiredByProject\n" +
                               "   > Could not find junit:junit:99.99.\n" +
                               "     Searched in the following locations:\n" +
                               "       $itemLinePrefix http://maven.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.pom\n" +
                               "       $itemLinePrefix http://maven.labs.intellij.net/repo1/junit/junit/99.99/junit-99.99.jar\n" +
                               "     Required by:\n" +
                               "         $requiredByProject\n" +
                               "\n" +
                               "Possible solution:\n" +
                               " - Declare repository providing the artifact, see the documentation at https://docs.gradle.org/current/userguide/declaring_repositories.html\n" +
                               "\n")
  }

  @Test
  fun `test startup build script errors with column info`() {
    importProject("apply plugin: 'java'\n" +
                  "dependencies { \n" +
                  "  testCompile group: 'junit', name: 'junit', version: '4.12\n" +
                  "}")

    assertSyncViewTreeEquals("-\n" +
                             " -failed\n" +
                             "  -build.gradle\n" +
                             "   expecting ''', found '\\n'")
  }

  @Test
  fun `test startup build script errors without column info`() {
    importProject("projects {}\n" +
                  "plugins { id 'java' }")

    assertSyncViewTreeEquals("-\n" +
                             " -failed\n" +
                             "  -build.gradle\n" +
                             "   only buildscript {} and other plugins {} script blocks are allowed before plugins {} blocks, no other statements are allowed")
  }

  @Test
  fun `test build script errors with stacktrace info`() {
    enableStackTraceImportingOption = true
    importProject("apply plugin: 'java'foo")

    assertSyncViewTreeEquals("-\n" +
                             " -failed\n" +
                             "  -build.gradle\n" +
                             "   Cannot get property 'foo' on null object")

    val filePath = FileUtil.toSystemDependentName(myProjectConfig.path)
    assertSyncViewSelectedNode("Cannot get property 'foo' on null object",
                               "Build file '$filePath' line: 1\n\n" +
                               "A problem occurred evaluating root project 'project'.\n" +
                               "> Cannot get property 'foo' on null object\n")
  }
}
