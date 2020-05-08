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
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.JavaHomeValidationStatus.Success

private data class GradleJvmProviderId(val projectSettings: GradleProjectSettings) : Id

fun getGradleJvmLookupProvider(project: Project, projectSettings: GradleProjectSettings) =
  SdkLookupProvider.getInstance(project, GradleJvmProviderId(projectSettings))

fun setupGradleJvm(project: Project, projectSettings: GradleProjectSettings, externalProjectPath: String) {
  setupGradleJvm(project, projectSettings, externalProjectPath, projectSettings.resolveGradleVersion())
}

fun setupGradleJvm(project: Project, projectSettings: GradleProjectSettings, externalProjectPath: String, gradleVersion: GradleVersion) {
  with(GradleJvmResolutionContext(project, externalProjectPath, gradleVersion)) {
    projectSettings.gradleJvm = findGradleJvm()
    if (projectSettings.gradleJvm != null) return
    when {
      canUseProjectSdk() -> projectSettings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
      canUseGradleJavaHomeJdk() -> projectSettings.gradleJvm = USE_GRADLE_JAVA_HOME
      canUseJavaHomeJdk() -> projectSettings.gradleJvm = ExternalSystemJdkUtil.USE_JAVA_HOME
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
}

fun updateGradleJvm(project: Project, externalProjectPath: String) {
  val settings = GradleSettings.getInstance(project)
  val projectSettings = settings.getLinkedProjectSettings(externalProjectPath) ?: return
  val gradleJvm = projectSettings.gradleJvm ?: return
  val gradleVersion = projectSettings.resolveGradleVersion()
  with(GradleJvmResolutionContext(project, externalProjectPath, gradleVersion)) {
    val projectSdk = projectSdk ?: return
    if (projectSdk.name != gradleJvm) return
    projectSettings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
  }
}

private class GradleJvmResolutionContext(
  val project: Project,
  val externalProjectPath: String,
  val gradleVersion: GradleVersion
) {
  val projectSdk: Sdk? by lazy { ProjectRootManager.getInstance(project).projectSdk }
}

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
  val projectSdk = projectSdk ?: return false
  val resolvedProjectSdk = ExternalSystemJdkUtil.resolveDependentJdk(projectSdk)
  return ExternalSystemJdkUtil.isValidJdk(resolvedProjectSdk)
}

private fun findRegisteredSdk(sdk: Sdk): Sdk? = runReadAction {
  val projectJdkTable = ProjectJdkTable.getInstance()
  projectJdkTable.findJdk(sdk.name, sdk.sdkType.name)
}
