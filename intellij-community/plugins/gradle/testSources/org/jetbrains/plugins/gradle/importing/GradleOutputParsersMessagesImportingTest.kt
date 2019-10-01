// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.GradleSystemSettings
import org.junit.Test

@Suppress("GrUnresolvedAccess")
open class GradleOutputParsersMessagesImportingTest : BuildViewMessagesImportingTestCase() {
  val itemLinePrefix by lazy { if (currentGradleVersion < GradleVersion.version("4.8")) " " else "-" }
  val isPerTaskOutputSupported by lazy { currentGradleVersion >= GradleVersion.version("4.7") }

  // do not inject repository
  override fun injectRepo(config: String): String = config

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
}
