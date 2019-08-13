// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.jarRepository.RemoteRepositoriesConfiguration
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.openapi.util.SystemInfo
import org.junit.Test

class GradleMavenRepositoriesImportTest: GradleImportingTestCase() {
  @Test
  fun `test maven repositories imported`() {
    currentExternalProjectSettings.isResolveExternalAnnotations = true
    try {
      importProject("""
      |repositories {
      |  maven {
      |    name "test"
      |    url "file:///tmp/repo"
      |  }
      |}
      |
    """.trimMargin())

      val repositoriesConfiguration = RemoteRepositoriesConfiguration.getInstance(myProject)

      val expectedUrl = if (SystemInfo.isWindows) {
        "file:////tmp/repo"
      } else {
        "file:/tmp/repo"
      }

      assertContainsElements(repositoriesConfiguration.repositories, RemoteRepositoryDescription("test", "test", expectedUrl))
    } finally {
      currentExternalProjectSettings.isResolveExternalAnnotations = false
    }
  }

}