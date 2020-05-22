// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.JAVA_HOME
import com.intellij.openapi.externalSystem.util.use
import org.junit.Test

class GradleProjectResolverTest : GradleProjectResolverTestCase() {
  @Test
  fun `test setup of project sdk for newly opened project`() {
    val projectPath = createTestDirectory("project")
    createTestFile("project/settings.gradle", GroovyBuilder().property("rootProject.name", "'project'").generate())
    createTestFile("project/build.gradle", GradleBuildScriptBuilderEx().withJavaPlugin().generate())

    val jdk = findRealTestSdk()

    environment.withVariables(JAVA_HOME to jdk.homePath) {
      withRegisteredSdk(jdk) {
        assertUnexpectedSdksRegistration {
          openOrImport(projectPath).use {
            assertProjectSdk(it, jdk)
          }
        }
      }
    }
  }

  @Test
  fun `test setup of project sdk for newly opened project in clean IDEA`() {
    val projectPath = createTestDirectory("project")
    createTestFile("project/settings.gradle", GroovyBuilder().property("rootProject.name", "'project'").generate())
    createTestFile("project/build.gradle", GradleBuildScriptBuilderEx().withJavaPlugin().generate())

    val jdk = findRealTestSdk()

    environment.withVariables(JAVA_HOME to jdk.homePath) {
      withoutRegisteredSdks {
        assertNewlyRegisteredSdks({ jdk }) {
          openOrImport(projectPath).use {
            assertProjectSdk(it, jdk)
          }
        }
      }
    }
  }
}