// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.JAVA_HOME
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtilTestCase.Companion.assertNewlyRegisteredSdks
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtilTestCase.Companion.assertUnexpectedSdksRegistration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtilTestCase.Companion.withRegisteredSdks
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtilTestCase.Companion.withoutRegisteredSdks
import com.intellij.openapi.roots.ui.configuration.SdkTestCase.TestSdkGenerator
import org.junit.Test

class GradleProjectResolverTest : GradleProjectResolverTestCase() {
  @Test
  fun `test setup of project sdk for newly opened project`() {
    val jdk = findRealTestSdk() ?: return
    createGradleSubProject()

    environment.withVariables(JAVA_HOME to jdk.homePath) {
      withRegisteredSdks(jdk) {
        assertUnexpectedSdksRegistration {
          loadProject()
          assertSdks(jdk.name, "project", "project.main", "project.test")
        }
      }
    }
  }

  @Test
  fun `test setup of project sdk for newly opened project in clean IDEA`() {
    val jdk = findRealTestSdk() ?: return
    createGradleSubProject()

    environment.withVariables(JAVA_HOME to jdk.homePath) {
      withoutRegisteredSdks {
        assertNewlyRegisteredSdks({ jdk }) {
          loadProject()
          assertSdks(jdk.name, "project", "project.main", "project.test")
        }
      }
    }
  }

  @Test
  fun `test project-module sdk replacing`() {
    val jdk = findRealTestSdk() ?: return
    val sdk = TestSdkGenerator.createNextSdk()
    createGradleSubProject()

    environment.withVariables(JAVA_HOME to jdk.homePath) {
      withRegisteredSdks(jdk, sdk) {
        assertUnexpectedSdksRegistration {
          loadProject()
          assertSdks(jdk.name, "project", "project.main", "project.test")

          withProjectSdk(sdk) {
            assertSdks(sdk.name, "project", "project.main", "project.test")

            reloadProject()
            assertSdks(sdk.name, "project", "project.main", "project.test")
          }
        }
      }
    }
  }
}