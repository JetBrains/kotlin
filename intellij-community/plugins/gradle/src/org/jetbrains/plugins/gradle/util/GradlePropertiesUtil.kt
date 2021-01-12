// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("GradlePropertiesUtil")
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.util.environment.Environment
import com.intellij.util.io.exists
import com.intellij.util.io.inputStream
import com.intellij.util.io.isFile
import org.jetbrains.plugins.gradle.settings.GradleSystemSettings
import org.jetbrains.plugins.gradle.util.GradleProperties.EMPTY
import org.jetbrains.plugins.gradle.util.GradleProperties.GradleProperty
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

const val USER_HOME = "user.home"
const val GRADLE_CACHE_DIR_NAME = ".gradle"
const val PROPERTIES_FILE_NAME = "gradle.properties"
const val GRADLE_JAVA_HOME_PROPERTY = "org.gradle.java.home"

fun getGradleProperties(externalProjectPath: Path): GradleProperties {
  return getPossiblePropertiesFiles(externalProjectPath)
    .asSequence()
    .map { it.toAbsolutePath().normalize() }
    .map(::loadGradleProperties)
    .reduce(::mergeGradleProperties)
}

private fun getPossiblePropertiesFiles(externalProjectPath: Path): List<Path> {
  return listOfNotNull(
    getGradleServiceDirectoryPath(),
    getGradleHomePropertiesPath(),
    getGradleProjectPropertiesPath(externalProjectPath)
  )
}

private fun getGradleServiceDirectoryPath(): Path? {
  val systemSettings = GradleSystemSettings.getInstance()
  val gradleUserHome = systemSettings.serviceDirectoryPath
  if (gradleUserHome == null) return null
  return Paths.get(gradleUserHome, PROPERTIES_FILE_NAME)
}

private fun getGradleHomePropertiesPath(): Path? {
  val gradleUserHome = Environment.getVariable(GradleConstants.SYSTEM_DIRECTORY_PATH_KEY)
  if (gradleUserHome != null) {
    return Paths.get(gradleUserHome, PROPERTIES_FILE_NAME)
  }

  val userHome = Environment.getProperty(USER_HOME)
  if (userHome != null) {
    return Paths.get(userHome, GRADLE_CACHE_DIR_NAME, PROPERTIES_FILE_NAME)
  }
  return null
}

private fun getGradleProjectPropertiesPath(externalProjectPath: Path): Path {
  return externalProjectPath.resolve(PROPERTIES_FILE_NAME)
}

private fun loadGradleProperties(propertiesPath: Path): GradleProperties {
  val properties = loadProperties(propertiesPath) ?: return EMPTY
  val javaHome = properties.getProperty(GRADLE_JAVA_HOME_PROPERTY)
  val javaHomeProperty = javaHome?.let { GradleProperty(it, propertiesPath.toString()) }
  return GradlePropertiesImpl(javaHomeProperty)
}

private fun loadProperties(propertiesFile: Path): Properties? {
  if (!propertiesFile.isFile() || !propertiesFile.exists()) {
    return null
  }

  val properties = Properties()
  propertiesFile.inputStream().use {
    properties.load(it)
  }
  return properties
}

private fun mergeGradleProperties(most: GradleProperties, other: GradleProperties): GradleProperties {
  return when {
    most is EMPTY -> other
    other is EMPTY -> most
    else -> GradlePropertiesImpl(most.javaHomeProperty ?: other.javaHomeProperty)
  }
}

private data class GradlePropertiesImpl(override val javaHomeProperty: GradleProperty<String>?) : GradleProperties