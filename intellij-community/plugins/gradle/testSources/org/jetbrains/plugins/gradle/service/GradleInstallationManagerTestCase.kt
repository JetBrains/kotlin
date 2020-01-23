// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightIdeaTestCase
import org.gradle.StartParameter
import org.gradle.util.GradleVersion
import org.gradle.wrapper.PathAssembler
import org.gradle.wrapper.WrapperConfiguration
import org.gradle.wrapper.WrapperExecutor
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import java.io.File
import java.net.URI
import java.util.*

abstract class GradleInstallationManagerTestCase : LightIdeaTestCase() {
  fun testGradleVersion(expectedVersion: GradleVersion,
                        distributionType: DistributionType,
                        wrapperVersionToGenerate: GradleVersion?) {
    val settings = GradleProjectSettings().apply {
      externalProjectPath = createUniqueTempDirectory()
      this.distributionType = distributionType
      if (wrapperVersionToGenerate != null) {
        gradleHome = generateFakeGradleWrapper(externalProjectPath, wrapperVersionToGenerate)
      }
    }

    val actualVersion = settings.resolveGradleVersion()
    assertEquals(expectedVersion, actualVersion)
  }

  private fun createUniqueTempDirectory(): String {
    val tempDirectory = FileUtil.join(FileUtil.getTempDirectory(), UUID.randomUUID().toString())
    FileUtil.createDirectory(File(tempDirectory))
    return tempDirectory
  }

  private fun generateFakeGradleWrapper(externalProjectPath: String, version: GradleVersion): String {
    val wrapperConfiguration = WrapperConfiguration()
    wrapperConfiguration.distribution = URI("http://gradle-${version.version}.com")

    val wrapperHome = FileUtil.join(externalProjectPath, "gradle", "wrapper")
    val wrapperJar = FileUtil.join(wrapperHome, "gradle-wrapper.jar")
    val wrapperProperties = FileUtil.join(wrapperHome, "gradle-wrapper.properties")
    val gradleUserHome = StartParameter.DEFAULT_GRADLE_USER_HOME
    val distributionPath = getLocalDistributionDir(gradleUserHome, wrapperConfiguration)
    val gradleHome = FileUtil.join(distributionPath, "gradle-${version.version}")
    val gradleJarFile = FileUtil.join(gradleHome, "lib", "gradle-${version.version}.jar")

    FileUtil.createDirectory(gradleUserHome)
    FileUtil.createIfNotExists(File(wrapperJar))
    FileUtil.createIfNotExists(File(wrapperProperties))
    FileUtil.createIfNotExists(File(gradleJarFile))

    storeWrapperProperties(wrapperProperties, wrapperConfiguration)

    return gradleHome
  }

  private fun getLocalDistributionDir(gradleUserHome: File, wrapperConfiguration: WrapperConfiguration): String {
    val pathAssembler = PathAssembler(gradleUserHome)
    val localDistribution = pathAssembler.getDistribution(wrapperConfiguration)
    return localDistribution.distributionDir.path
  }

  private fun storeWrapperProperties(wrapperProperties: String, wrapperConfiguration: WrapperConfiguration) {
    File(wrapperProperties).outputStream().use {
      val properties = Properties()
      properties.setProperty(WrapperExecutor.DISTRIBUTION_URL_PROPERTY, wrapperConfiguration.distribution.toString())
      properties.setProperty(WrapperExecutor.DISTRIBUTION_BASE_PROPERTY, wrapperConfiguration.distributionBase)
      properties.setProperty(WrapperExecutor.DISTRIBUTION_PATH_PROPERTY, wrapperConfiguration.distributionPath)
      properties.setProperty(WrapperExecutor.ZIP_STORE_BASE_PROPERTY, wrapperConfiguration.zipBase)
      properties.setProperty(WrapperExecutor.ZIP_STORE_PATH_PROPERTY, wrapperConfiguration.zipPath)
      properties.store(it, null)
    }
  }
}