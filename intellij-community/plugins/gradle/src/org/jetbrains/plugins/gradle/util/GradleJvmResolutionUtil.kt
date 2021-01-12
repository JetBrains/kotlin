// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("GradleJvmResolutionUtil")
@file:ApiStatus.Internal
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider.Id
import com.intellij.util.lang.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.JavaHomeValidationStatus.Success
import java.nio.file.Path
import java.nio.file.Paths

private data class GradleJvmProviderId(val projectSettings: GradleProjectSettings) : Id

fun getGradleJvmLookupProvider(project: Project, projectSettings: GradleProjectSettings) =
  SdkLookupProvider.getInstance(project, GradleJvmProviderId(projectSettings))

fun setupGradleJvm(project: Project, projectSettings: GradleProjectSettings, gradleVersion: GradleVersion) {
  val resolutionContext = GradleJvmResolutionContext(project, Paths.get(projectSettings.externalProjectPath), gradleVersion)
  projectSettings.gradleJvm = resolutionContext.findGradleJvm()
  if (projectSettings.gradleJvm != null) {
    return
  }

  when {
    resolutionContext.canUseProjectSdk() -> projectSettings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
    resolutionContext.canUseGradleJavaHomeJdk() -> projectSettings.gradleJvm = USE_GRADLE_JAVA_HOME
    resolutionContext.canUseJavaHomeJdk() -> projectSettings.gradleJvm = ExternalSystemJdkUtil.USE_JAVA_HOME
    else -> getGradleJvmLookupProvider(project, projectSettings)
      .newLookupBuilder()
      .withVersionFilter { isSupported(gradleVersion, it) }
      .withSdkType(ExternalSystemJdkUtil.getJavaSdkType())
      .withSdkHomeFilter { ExternalSystemJdkUtil.isValidJdk(it) }
      .onSdkNameResolved { sdk ->
        /* We have two types of sdk resolving:
         *  1. Download sdk manually
         *    a. by download action from SdkComboBox
         *    b. by sdk downloader
         *    c. by action that detects incorrect project sdk
         *  2. Lookup sdk (search in fs, download and etc)
         *    a. search in fs, search in sdk table and etc
         *    b. download
         *
         * All download actions generates fake (invalid) sdk and puts it to jdk table.
         * This code allows to avoid some irregular conflicts
         * For example: strange duplications in SdkComboBox or unexpected modifications of gradleJvm
         */
        val fakeSdk = sdk?.let(::findRegisteredSdk)
        if (fakeSdk != null && projectSettings.gradleJvm == null) {
          projectSettings.gradleJvm = fakeSdk.name
        }
      }
      .onSdkResolved { sdk ->
        if (projectSettings.gradleJvm == null) {
          projectSettings.gradleJvm = sdk?.name
        }
      }
      .executeLookup()
  }
}

fun updateGradleJvm(project: Project, externalProjectPath: String) {
  val settings = GradleSettings.getInstance(project)
  val projectSettings = settings.getLinkedProjectSettings(externalProjectPath) ?: return
  val gradleJvm = projectSettings.gradleJvm ?: return
  val projectRootManager = ProjectRootManager.getInstance(project)
  val projectSdk = projectRootManager.projectSdk ?: return
  if (projectSdk.name != gradleJvm) return
  projectSettings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
}

fun suggestGradleVersion(project: Project): GradleVersion? {
  val gradleVersion = findGradleVersion(project)
  if (gradleVersion != null) return gradleVersion
  val projectJdk = resolveProjectJdk(project) ?: return null
  if (!ExternalSystemJdkUtil.isValidJdk(projectJdk)) return null
  val javaVersion = JavaVersion.tryParse(projectJdk.versionString) ?: return null
  return suggestGradleVersion(javaVersion)
}

/**
 * @see org.jetbrains.plugins.gradle.util.isSupported
 */
private fun suggestGradleVersion(javaVersion: JavaVersion): GradleVersion? {
  val version = javaVersion.feature
  return when {
    version >= 8 /* ..14 */ -> GradleVersion.version("6.3")
    version == 7 -> GradleVersion.version("4.1")
    version == 6 -> GradleVersion.version("3.0")
    else -> null
  }
}

private fun findGradleVersion(project: Project): GradleVersion? {
  val settings = GradleSettings.getInstance(project)
  return settings.linkedProjectsSettings.asSequence()
    .mapNotNull { it.resolveGradleVersion() }
    .firstOrNull()
}

private class GradleJvmResolutionContext(
  val project: Project,
  val externalProjectPath: Path,
  val gradleVersion: GradleVersion
)

private fun GradleJvmResolutionContext.canUseGradleJavaHomeJdk(): Boolean {
  val properties = getGradleProperties(externalProjectPath)
  val javaHome = properties.javaHomeProperty?.value
  val validationStatus = validateGradleJavaHome(gradleVersion, javaHome)
  return validationStatus is Success
}

private fun GradleJvmResolutionContext.canUseJavaHomeJdk(): Boolean {
  val javaHome = ExternalSystemJdkUtil.getJavaHome()
  val validationStatus = validateGradleJavaHome(gradleVersion, javaHome)
  return validationStatus is Success
}

private fun GradleJvmResolutionContext.findGradleJvm(): String? {
  val settings = GradleSettings.getInstance(project)
  return settings.linkedProjectsSettings.asSequence()
    .mapNotNull { it.gradleJvm }
    .firstOrNull()
}

private fun GradleJvmResolutionContext.canUseProjectSdk(): Boolean {
  val projectJdk = resolveProjectJdk(project) ?: return false
  if (!ExternalSystemJdkUtil.isValidJdk(projectJdk)) return false
  val versionString = projectJdk.versionString ?: return false
  return isSupported(gradleVersion, versionString)
}

private fun resolveProjectJdk(project: Project): Sdk? {
  val projectRootManager = ProjectRootManager.getInstance(project)
  val projectSdk = projectRootManager.projectSdk ?: return null
  return ExternalSystemJdkUtil.resolveDependentJdk(projectSdk)
}


private fun findRegisteredSdk(sdk: Sdk): Sdk? = runReadAction {
  val projectJdkTable = ProjectJdkTable.getInstance()
  projectJdkTable.findJdk(sdk.name, sdk.sdkType.name)
}
