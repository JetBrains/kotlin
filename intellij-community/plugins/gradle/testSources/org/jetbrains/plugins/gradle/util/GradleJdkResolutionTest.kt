// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK
import com.intellij.openapi.externalSystem.util.environment.Environment.Companion.USER_HOME
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_DIRECTORY_PATH_KEY
import org.junit.Test

class GradleJdkResolutionTest : GradleJdkResolutionTestCase() {
  @Test
  fun `test simple gradle jvm resolution`() {
    withGradleProperties(externalProjectPath, java = latestSdk) {
      assertGradleJvmSuggestion(expected = latestSdk, expectsSdkRegistration = true)
    }
    withRegisteredSdks(earliestSdk, latestSdk, unsupportedSdk) {
      withGradleLinkedProject(java = earliestSdk) {
        assertGradleJvmSuggestion(expected = earliestSdk)
      }
    }
    assertGradleJvmSuggestion(expected = USE_PROJECT_JDK, projectSdk = latestSdk)
    environment.withVariables(JAVA_HOME to latestSdk.homePath) {
      assertGradleJvmSuggestion(expected = latestSdk, expectsSdkRegistration = true)
    }
    withRegisteredSdks(earliestSdk, latestSdk, unsupportedSdk) {
      assertGradleJvmSuggestion(expected = latestSdk)
    }
    assertGradleJvmSuggestion(expected = latestSdk, expectsSdkRegistration = true)
  }

  @Test
  fun `test gradle jvm resolution (linked project)`() {
    registerSdks(earliestSdk, latestSdk, unsupportedSdk)
    withGradleLinkedProject(java = earliestSdk) {
      assertGradleJvmSuggestion(expected = earliestSdk)
    }
    withGradleLinkedProject(java = latestSdk) {
      assertGradleJvmSuggestion(expected = latestSdk)
    }
    withGradleLinkedProject(java = unsupportedSdk) {
      assertGradleJvmSuggestion(expected = latestSdk)
    }
  }

  @Test
  fun `test gradle jvm resolution (project sdk)`() {
    assertGradleJvmSuggestion(expected = USE_PROJECT_JDK, projectSdk = earliestSdk)
    assertGradleJvmSuggestion(expected = USE_PROJECT_JDK, projectSdk = latestSdk)
    assertGradleJvmSuggestion(expected = latestSdk, projectSdk = unsupportedSdk, expectsSdkRegistration = true)
  }

  @Test
  fun `test gradle jvm resolution (java home)`() {
    environment.variables(JAVA_HOME to earliestSdk.homePath)
    assertGradleJvmSuggestion(expected = earliestSdk, expectsSdkRegistration = true)
    environment.variables(JAVA_HOME to latestSdk.homePath)
    assertGradleJvmSuggestion(expected = latestSdk, expectsSdkRegistration = true)
    environment.variables(JAVA_HOME to unsupportedSdk.homePath)
    assertGradleJvmSuggestion(expected = latestSdk, expectsSdkRegistration = true)
  }

  @Test
  fun `test gradle jvm resolution (gradle properties)`() {
    withGradleProperties(externalProjectPath, java = earliestSdk) {
      assertGradleJvmSuggestion(expected = earliestSdk, expectsSdkRegistration = true)
    }
    withGradleProperties(externalProjectPath, java = latestSdk) {
      assertGradleJvmSuggestion(expected = latestSdk, expectsSdkRegistration = true)
    }
    withGradleProperties(externalProjectPath, java = unsupportedSdk) {
      assertGradleJvmSuggestion(expected = unsupportedSdk, expectsSdkRegistration = true)
    }
  }

  @Test
  fun `test gradle jvm resolution (reference resolving)`() {
    registerSdk(latestSdk)
    withGradleProperties(externalProjectPath, java = latestSdk) {
      assertGradleJvmSuggestion(expected = USE_PROJECT_JDK, projectSdk = latestSdk)
    }
    withGradleLinkedProject(java = latestSdk) {
      assertGradleJvmSuggestion(expected = USE_PROJECT_JDK, projectSdk = latestSdk)
    }
    assertGradleJvmSuggestion(expected = USE_PROJECT_JDK, projectSdk = latestSdk)
    environment.withVariables(JAVA_HOME to latestSdk.homePath) {
      assertGradleJvmSuggestion(expected = USE_PROJECT_JDK, projectSdk = latestSdk)
    }
  }

  @Test
  fun `test gradle properties resolution (project properties)`() {
    assertGradleProperties(java = null)
    withGradleProperties(externalProjectPath, java = earliestSdk) {
      assertGradleJvmSuggestion(expected = earliestSdk, expectsSdkRegistration = true)
    }
    withGradleProperties(externalProjectPath, java = latestSdk) {
      assertGradleProperties(java = latestSdk)
    }
    withGradleProperties(externalProjectPath, java = null) {
      assertGradleProperties(java = null)
    }
  }

  @Test
  fun `test gradle properties resolution (user_home properties)`() {
    environment.properties(USER_HOME to userHome)
    withGradleProperties(userCache, java = earliestSdk) {
      assertGradleProperties(java = earliestSdk)
      withGradleProperties(externalProjectPath, java = latestSdk) {
        assertGradleProperties(java = earliestSdk)
      }
    }
    withGradleProperties(userCache, java = latestSdk) {
      assertGradleProperties(java = latestSdk)
      withGradleProperties(externalProjectPath, java = null) {
        assertGradleProperties(java = latestSdk)
      }
    }
    withGradleProperties(userCache, java = null) {
      assertGradleProperties(java = null)
      withGradleProperties(externalProjectPath, java = latestSdk) {
        assertGradleProperties(java = latestSdk)
      }
    }
  }

  @Test
  fun `test gradle properties resolution (GRADLE_USER_HOME properties)`() {
    environment.variables(SYSTEM_DIRECTORY_PATH_KEY to gradleUserHome)
    withGradleProperties(gradleUserHome, java = earliestSdk) {
      assertGradleProperties(java = earliestSdk)
      withGradleProperties(externalProjectPath, java = latestSdk) {
        assertGradleProperties(java = earliestSdk)
      }
    }
    withGradleProperties(gradleUserHome, java = latestSdk) {
      assertGradleProperties(java = latestSdk)
      withGradleProperties(externalProjectPath, java = null) {
        assertGradleProperties(java = latestSdk)
      }
    }
    withGradleProperties(gradleUserHome, java = null) {
      assertGradleProperties(java = null)
      withGradleProperties(externalProjectPath, java = latestSdk) {
        assertGradleProperties(java = latestSdk)
      }
    }
  }

  @Test
  fun `test gradle properties resolution (GRADLE_USER_HOME overrides user_home)`() {
    environment.properties(USER_HOME to userHome)
    environment.variables(SYSTEM_DIRECTORY_PATH_KEY to gradleUserHome)
    withGradleProperties(gradleUserHome, java = earliestSdk) {
      withGradleProperties(userCache, java = latestSdk) {
        assertGradleProperties(java = earliestSdk)
      }
    }
    withGradleProperties(gradleUserHome, java = null) {
      withGradleProperties(userCache, java = latestSdk) {
        assertGradleProperties(java = null)
      }
    }
  }
}