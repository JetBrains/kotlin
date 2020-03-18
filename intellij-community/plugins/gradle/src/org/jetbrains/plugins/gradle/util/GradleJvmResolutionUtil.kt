// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("GradleJvmResolutionUtil")
@file:ApiStatus.Internal

package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.environment.Environment
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.JavaHomeValidationStatus.Success

const val JAVA_HOME = "JAVA_HOME"

fun suggestGradleJvm(project: Project, projectSdk: Sdk?, externalProjectPath: String, gradleVersion: GradleVersion): String? {
  with(GradleJvmResolutionContext(project, projectSdk, externalProjectPath, gradleVersion)) {
    val suggestedGradleJvm =
      getOrAddGradleJavaHomeJdkReference()
      ?: getOrAddEnvJavaHomeJdkReference()
      ?: getGradleJdkReference()
      ?: getProjectJdkReference()
      ?: getMostRecentJdkReference()
      ?: getAndAddExternalJdkReference()
      ?: return null
    return resolveReference(suggestedGradleJvm)
  }
}

fun updateGradleJvm(project: Project, externalProjectPath: String) {
  val settings = GradleSettings.getInstance(project)
  val projectSettings = settings.getLinkedProjectSettings(externalProjectPath) ?: return
  val gradleJvm = projectSettings.gradleJvm ?: return
  val projectRootManager = ProjectRootManager.getInstance(project)
  val projectSdk = projectRootManager.projectSdk ?: return
  val gradleVersion = projectSettings.resolveGradleVersion()
  with(GradleJvmResolutionContext(project, projectSdk, externalProjectPath, gradleVersion)) {
    if (projectSdk.name != gradleJvm) return
    if (!isValidAndSupported(projectSdk)) return
    projectSettings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
  }
}

private class GradleJvmResolutionContext(
  val project: Project,
  projectSdk: Sdk?,
  val externalProjectPath: String,
  val gradleVersion: GradleVersion
) {
  val possibleGradleJvms: List<Sdk> by lazy { resolvePossibleGradleJvms() }
  val projectSdk: Sdk? by lazy { projectSdk ?: ProjectRootManager.getInstance(project).projectSdk }
}

private fun GradleJvmResolutionContext.resolveReference(sdk: Sdk?): String? {
  return when (sdk) {
    null -> null
    projectSdk -> ExternalSystemJdkUtil.USE_PROJECT_JDK
    else -> sdk.name
  }
}

private fun GradleJvmResolutionContext.isValidAndSupported(homePath: String): Boolean {
  if (!ExternalSystemJdkUtil.isValidJdk(homePath)) return false
  val javaSdkType = ExternalSystemJdkUtil.getJavaSdkType()
  val versionString = javaSdkType.getVersionString(homePath) ?: return false
  return isSupported(gradleVersion, versionString)
}

private fun GradleJvmResolutionContext.isValidAndSupported(sdk: Sdk): Boolean {
  if (!ExternalSystemJdkUtil.isValidJdk(sdk)) return false
  val versionString = sdk.versionString ?: return false
  return isSupported(gradleVersion, versionString)
}

private fun GradleJvmResolutionContext.resolvePossibleGradleJvms(): List<Sdk> {
  val projectJdkTable = ProjectJdkTable.getInstance()
  val javaSdkType = ExternalSystemJdkUtil.getJavaSdkType()
  return projectJdkTable.getSdksOfType(javaSdkType)
    .filter { isValidAndSupported(it) }
}

private fun findOrAddJdk(homePath: String): Sdk? {
  val projectJdkTable = ProjectJdkTable.getInstance()
  val javaSdkType = ExternalSystemJdkUtil.getJavaSdkType()
  val canonicalHomePath = FileUtil.toCanonicalPath(homePath)
  val foundJdk = projectJdkTable.getSdksOfType(javaSdkType)
    .find { FileUtil.toCanonicalPath(it.homePath) == canonicalHomePath }
  if (foundJdk != null) return foundJdk
  return ExternalSystemJdkUtil.addJdk(canonicalHomePath)
}

private fun GradleJvmResolutionContext.findOrAddGradleJdk(homePath: String): Sdk? {
  val canonicalHomePath = FileUtil.toCanonicalPath(homePath)
  val foundJdk = possibleGradleJvms.find { FileUtil.toCanonicalPath(it.homePath) == canonicalHomePath }
  if (foundJdk != null) return foundJdk
  return ExternalSystemJdkUtil.addJdk(canonicalHomePath)
}

private fun GradleJvmResolutionContext.getGradleJdkReference(): Sdk? {
  val settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
  return settings.getLinkedProjectsSettings()
    .asSequence()
    .filterIsInstance<GradleProjectSettings>()
    .mapNotNull { ps -> possibleGradleJvms.find { it.name == ps.gradleJvm } }
    .firstOrNull()
}

private fun GradleJvmResolutionContext.getOrAddGradleJavaHomeJdkReference(): Sdk? {
  val properties = getGradleProperties(externalProjectPath)
  val javaHomeProperty = properties.javaHomeProperty ?: return null
  val javaHome = javaHomeProperty.value
  return findOrAddJdk(javaHome)
}

private fun GradleJvmResolutionContext.getOrAddEnvJavaHomeJdkReference(): Sdk? {
  val javaHome = Environment.getEnvVariable(JAVA_HOME)
  val validationStatus = validateGradleJavaHome(gradleVersion, javaHome)
  if (validationStatus !is Success) return null
  return findOrAddGradleJdk(validationStatus.javaHome)
}

private fun GradleJvmResolutionContext.getProjectJdkReference(): Sdk? {
  val projectSdk = projectSdk ?: return null
  val resolvedProjectSdk = ExternalSystemJdkUtil.resolveDependentJdk(projectSdk)
  if (!isValidAndSupported(resolvedProjectSdk)) return null
  return resolvedProjectSdk
}

private fun GradleJvmResolutionContext.getMostRecentJdkReference(): Sdk? {
  val javaSdkType = ExternalSystemJdkUtil.getJavaSdkType()
  return possibleGradleJvms.maxWith(javaSdkType.versionComparator())
}

private fun GradleJvmResolutionContext.getAndAddExternalJdkReference(): Sdk? {
  val javaSdkType = ExternalSystemJdkUtil.getJavaSdkType()
  val versionComparator = javaSdkType.versionStringComparator()
  return ExternalSystemJdkUtil.suggestJdkHomePaths()
    .filter { isValidAndSupported(it) }
    .map { it to javaSdkType.getVersionString(it) }
    .maxWith(Comparator { (_, v1), (_, v2) -> versionComparator.compare(v1, v2) })
    ?.let { (homePath, _) -> findOrAddGradleJdk(homePath) }
}