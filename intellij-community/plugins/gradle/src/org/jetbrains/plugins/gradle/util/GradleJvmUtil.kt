// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("GradleJvmUtil")
@file:ApiStatus.Internal
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.service.execution.createJdkInfo
import com.intellij.openapi.externalSystem.service.execution.nonblockingResolveJdkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider.SdkInfo
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Paths

const val USE_GRADLE_JAVA_HOME = "#GRADLE_JAVA_HOME"

fun SdkLookupProvider.nonblockingResolveGradleJvmInfo(project: Project, externalProjectPath: String?, gradleJvm: String?): SdkInfo {
  val projectRootManager = ProjectRootManager.getInstance(project)
  val projectSdk = projectRootManager.projectSdk
  return nonblockingResolveGradleJvmInfo(projectSdk, externalProjectPath, gradleJvm)
}

fun SdkLookupProvider.nonblockingResolveGradleJvmInfo(projectSdk: Sdk?, externalProjectPath: String?, gradleJvm: String?): SdkInfo {
  return when (gradleJvm) {
    USE_GRADLE_JAVA_HOME -> createJdkInfo(GRADLE_JAVA_HOME_PROPERTY, getGradleJavaHome(externalProjectPath))
    else -> nonblockingResolveJdkInfo(projectSdk, gradleJvm)
  }
}

fun getGradleJavaHome(externalProjectPath: String?): String? {
  if (externalProjectPath == null) {
    return null
  }
  val properties = getGradleProperties(Paths.get(externalProjectPath))
  val javaHomeProperty = properties.javaHomeProperty ?: return null
  return javaHomeProperty.value
}