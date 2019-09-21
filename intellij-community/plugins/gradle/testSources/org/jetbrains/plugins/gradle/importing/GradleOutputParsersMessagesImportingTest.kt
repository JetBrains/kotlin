// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import org.gradle.util.GradleVersion
import org.junit.Test

@Suppress("GrUnresolvedAccess")
open class GradleOutputParsersMessagesImportingTest : BuildViewMessagesImportingTestCase() {

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
}
