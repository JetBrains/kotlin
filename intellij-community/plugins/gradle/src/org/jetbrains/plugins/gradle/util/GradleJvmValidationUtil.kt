// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:ApiStatus.Internal
@file:JvmName("GradleJvmValidationUtil")

package org.jetbrains.plugins.gradle.util

import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.ide.util.EditorHelper
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtil.*
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.util.lang.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.service.project.GradleNotification.NOTIFICATION_GROUP
import org.jetbrains.plugins.gradle.util.GradleProperties.GradleProperty
import java.io.File
import java.nio.file.Path
import javax.swing.event.HyperlinkEvent

fun validateJavaHome(project: Project, externalProjectPath: Path, gradleVersion: GradleVersion) {
  val gradleProperties = getGradleProperties(externalProjectPath)
  val javaHomeProperty = gradleProperties.javaHomeProperty
  if (javaHomeProperty != null) {
    val javaHome = javaHomeProperty.value
    when (val validationStatus = validateGradleJavaHome(gradleVersion, javaHome)) {
      JavaHomeValidationStatus.Invalid -> notifyInvalidGradleJavaHomeInfo(project, javaHomeProperty, validationStatus)
      is JavaHomeValidationStatus.Unsupported -> notifyInvalidGradleJavaHomeInfo(project, javaHomeProperty, validationStatus)
    }
  }
  else {
    val javaHome = ExternalSystemJdkUtil.getJavaHome()
    when (val validationStatus = validateGradleJavaHome(gradleVersion, javaHome)) {
      JavaHomeValidationStatus.Invalid -> notifyInvalidJavaHomeInfo(project, validationStatus)
      is JavaHomeValidationStatus.Unsupported -> notifyInvalidJavaHomeInfo(project, validationStatus)
    }
  }
}

fun validateGradleJavaHome(gradleVersion: GradleVersion, javaHome: String?): JavaHomeValidationStatus {
  if (javaHome == null) return JavaHomeValidationStatus.Undefined
  if (!ExternalSystemJdkUtil.isValidJdk(javaHome)) return JavaHomeValidationStatus.Invalid
  val javaSdkType = ExternalSystemJdkUtil.getJavaSdkType()
  val versionString = javaSdkType.getVersionString(javaHome) ?: return JavaHomeValidationStatus.Invalid
  val javaVersion = JavaVersion.tryParse(versionString) ?: return JavaHomeValidationStatus.Invalid
  if (!isSupported(gradleVersion, versionString)) return JavaHomeValidationStatus.Unsupported(javaVersion, gradleVersion)
  return JavaHomeValidationStatus.Success(javaHome)
}

/**
 * @see org.jetbrains.plugins.gradle.util.suggestGradleVersion
 */
fun isSupported(gradleVersion: GradleVersion, javaVersionString: String): Boolean {
  val version = JavaVersion.tryParse(javaVersionString) ?: return false
  return when {
    gradleVersion >= GradleVersion.version("6.3") -> version.feature >= 8 // ..14
    gradleVersion >= GradleVersion.version("6.0") -> version.feature in 8..13
    gradleVersion >= GradleVersion.version("5.4.1") -> version.feature in 8..12
    gradleVersion >= GradleVersion.version("5.0") -> version.feature in 8..11
    gradleVersion >= GradleVersion.version("4.1") -> version.feature in 7..9
    gradleVersion >= GradleVersion.version("4.0") -> version.feature in 7..8
    else -> version.feature in 6..8
  }
}

private fun notifyInvalidGradleJavaHomeInfo(
  project: Project,
  javaHomeProperty: GradleProperty<String>,
  reason: JavaHomeValidationStatus
) {
  val propertyLocation = createLinkToFile(project, javaHomeProperty.location)
  val notificationContent = GradleBundle.message("gradle.notifications.java.home.property.content", propertyLocation)
  notifyInvalidGradleJvmInfo(project, notificationContent, reason)
}

private fun notifyInvalidJavaHomeInfo(project: Project, reason: JavaHomeValidationStatus) {
  val notificationContent = GradleBundle.message("gradle.notifications.java.home.variable.content")
  notifyInvalidGradleJvmInfo(project, notificationContent, reason)
}

private fun createLinkToFile(project: Project, path: String): String {
  val projectDirectory = project.guessProjectDir()
  val projectPath = projectDirectory?.path?.let { toSystemDependentName(it) }
  val filePath = toSystemDependentName(path)
  val presentablePath = when {
    projectPath == null -> getLocationRelativeToUserHome(filePath)
    isAncestor(projectPath, filePath, true) -> getRelativePath(projectPath, filePath, File.separatorChar)
    else -> getLocationRelativeToUserHome(filePath)
  }
  return "<a href='$path'>$presentablePath</a>"
}

private fun notifyInvalidGradleJvmInfo(project: Project, notificationHint: String, reason: JavaHomeValidationStatus) {
  val notificationTitle = GradleBundle.message("gradle.notifications.java.home.invalid.title")
  var notificationContent = notificationHint
  if (reason is JavaHomeValidationStatus.Unsupported) {
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
  val notification = NOTIFICATION_GROUP.createNotification(notificationTitle, notificationContent, INFORMATION, hyperLinkProcessor)
  notification.notify(project)
}

sealed class JavaHomeValidationStatus {
  object Invalid : JavaHomeValidationStatus()
  object Undefined : JavaHomeValidationStatus()
  class Unsupported(val javaVersion: JavaVersion, val gradleVersion: GradleVersion) : JavaHomeValidationStatus()
  class Success(val javaHome: String) : JavaHomeValidationStatus()
}