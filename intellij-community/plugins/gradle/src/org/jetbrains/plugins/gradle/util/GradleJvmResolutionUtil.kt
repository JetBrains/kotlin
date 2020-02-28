// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("GradleJvmResolutionUtil")

package org.jetbrains.plugins.gradle.util

import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.ide.util.EditorHelper
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.util.lang.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.service.project.GradleNotification.NOTIFICATION_GROUP
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleProperties.GradleProperty
import javax.swing.event.HyperlinkEvent

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

sealed class FailureReason {
  object Invalid : FailureReason()
  data class Unsupported(val javaVersion: JavaVersion, val gradleVersion: GradleVersion) : FailureReason()
}

private fun GradleJvmResolutionContext.resolveReference(sdk: Sdk?): String? {
  return when (sdk) {
    null -> null
    projectSdk -> ExternalSystemJdkUtil.USE_PROJECT_JDK
    else -> sdk.name
  }
}

private fun GradleJvmResolutionContext.checkGradleJvm(javaHome: String): FailureReason? {
  if (!ExternalSystemJdkUtil.isValidJdk(javaHome)) return FailureReason.Invalid
  val javaSdkType = ExternalSystemJdkUtil.getJavaSdkType()
  val versionString = javaSdkType.getVersionString(javaHome) ?: return FailureReason.Invalid
  val javaVersion = JavaVersion.tryParse(versionString) ?: return FailureReason.Invalid
  if (!isSupported(versionString)) return FailureReason.Unsupported(javaVersion, gradleVersion)
  return null
}

private fun GradleJvmResolutionContext.isValidAndSupported(homePath: String): Boolean {
  if (!ExternalSystemJdkUtil.isValidJdk(homePath)) return false
  val javaSdkType = ExternalSystemJdkUtil.getJavaSdkType()
  val versionString = javaSdkType.getVersionString(homePath) ?: return false
  return isSupported(versionString)
}

private fun GradleJvmResolutionContext.isValidAndSupported(sdk: Sdk): Boolean {
  if (!ExternalSystemJdkUtil.isValidJdk(sdk)) return false
  val versionString = sdk.versionString ?: return false
  return isSupported(versionString)
}

private fun GradleJvmResolutionContext.isSupported(versionString: String): Boolean {
  val version = JavaVersion.tryParse(versionString) ?: return false
  return when {
    gradleVersion >= GradleVersion.version("6.0") -> version.feature in 8..13
    gradleVersion >= GradleVersion.version("5.4.1") -> version.feature in 8..12
    gradleVersion >= GradleVersion.version("5.0") -> version.feature in 8..11
    gradleVersion >= GradleVersion.version("4.1") -> version.feature in 7..9
    gradleVersion >= GradleVersion.version("4.0") -> version.feature in 7..8
    else -> version.feature in 6..8
  }
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
  val failureReason = checkGradleJvm(javaHome)
  if (failureReason != null) {
    notifyInvalidGradleJavaHomeWarning(javaHomeProperty, failureReason)
  }
  return findOrAddJdk(javaHome)
}

private fun GradleJvmResolutionContext.getOrAddEnvJavaHomeJdkReference(): Sdk? {
  val javaHome = Environment.getEnvVariable(JAVA_HOME)
  if (javaHome == null) {
    notifyUndefinedJavaHomeWarning()
    return null
  }
  val failureReason = checkGradleJvm(javaHome)
  if (failureReason != null) {
    notifyInvalidJavaHomeWarning(failureReason)
    return null
  }
  return findOrAddGradleJdk(javaHome)
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

private fun GradleJvmResolutionContext.notifyInvalidGradleJavaHomeWarning(javaHomeProperty: GradleProperty<String>, reason: FailureReason) {
  val propertyLocation = createLinkToFile(javaHomeProperty.location)
  val notificationContent = GradleBundle.message("gradle.notifications.java.home.property.content", propertyLocation)
  notifyInvalidGradleJvmWarning(notificationContent, reason)
}

private fun GradleJvmResolutionContext.notifyInvalidJavaHomeWarning(reason: FailureReason) {
  val notificationContent = GradleBundle.message("gradle.notifications.java.home.variable.content")
  notifyInvalidGradleJvmWarning(notificationContent, reason)
}

private fun GradleJvmResolutionContext.createLinkToFile(path: String): String {
  val presentablePath = when {
    FileUtil.isAncestor(externalProjectPath, path, true) -> FileUtil.getRelativePath(externalProjectPath, path, '/')
    else -> FileUtil.getLocationRelativeToUserHome(path)
  }
  return "<a href='$path'>$presentablePath</a>"
}

private fun GradleJvmResolutionContext.notifyInvalidGradleJvmWarning(notificationHint: String, reason: FailureReason) {
  val notificationTitle = GradleBundle.message("gradle.notifications.java.home.invalid.title")
  var notificationContent = notificationHint
  if (reason is FailureReason.Unsupported) {
    val javaVersion = reason.javaVersion.toString()
    val gradleVersion = reason.gradleVersion.version
    val additionalFailureHint = GradleBundle.message("gradle.notifications.java.home.unsupported.content", javaVersion, gradleVersion)
    notificationContent = "$additionalFailureHint $notificationContent"
  }
  val hyperLinkProcessor = object : NotificationListener.Adapter() {
    override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
      val file = LocalFileSystem.getInstance().findFileByPath(e.description) ?: return
      ProjectViewSelectInTarget.select(project, file, ProjectViewPane.ID, null, file, true)
      val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
      EditorHelper.openInEditor(psiFile)
    }
  }
  val notification = NOTIFICATION_GROUP.createNotification(notificationTitle, notificationContent, WARNING, hyperLinkProcessor)
  notification.notify(project)
}

private fun GradleJvmResolutionContext.notifyUndefinedJavaHomeWarning() {
  val notificationTitle = GradleBundle.message("gradle.notifications.java.home.undefined.title")
  val notificationContent = GradleBundle.message("gradle.notifications.java.home.undefined.content")
  val notification = NOTIFICATION_GROUP.createNotification(notificationTitle, notificationContent, WARNING)
  notification.notify(project)
}