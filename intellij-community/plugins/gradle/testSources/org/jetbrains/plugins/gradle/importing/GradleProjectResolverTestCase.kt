// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtilTestCase
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.testFramework.PlatformTestUtil
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.util.isSupported
import java.io.File

abstract class GradleProjectResolverTestCase : ExternalSystemJdkUtilTestCase() {

  private lateinit var tempDirectory: File

  override fun setUp() {
    super.setUp()

    tempDirectory = FileUtilRt.generateRandomTemporaryPath()
  }

  fun createTestDirectory(relativePath: String): String {
    val file = File(tempDirectory, relativePath)
    file.mkdirs()
    return file.path
  }

  fun createTestFile(relativePath: String, content: String) {
    val file = File(tempDirectory, relativePath)
    file.parentFile.mkdirs()
    file.createNewFile()
    file.writeText(content)
  }

  fun findRealTestSdk(): TestSdk {
    val jdkType = JavaSdk.getInstance()
    val jdkInfo = jdkType.suggestHomePaths().asSequence()
      .map { createSdkInfo(jdkType, it) }
      .filter { ExternalSystemJdkUtil.isValidJdk(it.homePath) }
      .filter { isSupported(GradleVersion.current(), it.versionString) }
      .first()
    return TestSdkGenerator.createSdk(jdkInfo)
  }

  private fun createSdkInfo(sdkType: SdkType, homePath: String): TestSdkGenerator.SdkInfo {
    val name = sdkType.suggestSdkName(null, homePath)
    val versionString = sdkType.getVersionString(homePath)!!
    return TestSdkGenerator.SdkInfo(name, versionString, homePath)
  }

  fun openOrImport(projectPath: String): Project {
    return ProjectUtil.openOrImport(projectPath, null, false)!!.also {
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
  }

  fun assertProjectSdk(project: Project, expected: TestSdk) {
    val projectRootManager = ProjectRootManager.getInstance(project)
    assertSdk(expected, projectRootManager.projectSdk!!)
  }
}