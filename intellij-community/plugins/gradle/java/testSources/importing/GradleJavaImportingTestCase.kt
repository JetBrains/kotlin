// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.compiler.CompilerConfiguration
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.test.ExternalSystemImportingTestCase
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersionUtil
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LanguageLevelModuleExtensionImpl
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.pom.java.LanguageLevel

abstract class GradleJavaImportingTestCase : GradleImportingTestCase() {

  private fun getLanguageLevelForProjectSdk(project: Project): LanguageLevel? {
    val projectRootManager = ProjectRootManager.getInstance(project)
    val projectSdk = projectRootManager.projectSdk
    val projectSdkVersion = JavaSdkVersionUtil.getJavaSdkVersion(projectSdk) ?: return null
    return projectSdkVersion.maxLanguageLevel
  }

  private fun getLanguageLevelForProject(project: Project): LanguageLevel? {
    val languageLevelProjectExtension = LanguageLevelProjectExtension.getInstance(project)
    if (languageLevelProjectExtension.isDefault) {
      return getLanguageLevelForProjectSdk(project)
    }
    else {
      return languageLevelProjectExtension.languageLevel
    }
  }

  private fun getLanguageLevelForModule(module: Module): LanguageLevel? {
    val moduleExtension = LanguageLevelModuleExtensionImpl.getInstance(module)
    val moduleLanguageLevel = moduleExtension.languageLevel
    return moduleLanguageLevel ?: getLanguageLevelForProject(module.project)
  }

  fun getLanguageLevelForModule(moduleName: String): LanguageLevel? {
    val module = getModule(moduleName)
    return getLanguageLevelForModule(module)
  }

  private fun getBytecodeTargetLevelForProject(): String? {
    val compilerConfiguration = CompilerConfiguration.getInstance(myProject)
    return compilerConfiguration.projectBytecodeTarget
  }

  fun getBytecodeTargetLevelForModule(moduleName: String): String? {
    val compilerConfiguration = CompilerConfiguration.getInstance(myProject)
    val module = getModule(moduleName)
    return compilerConfiguration.getBytecodeTargetLevel(module)
  }

  fun getSdkForModule(moduleName: String): Sdk? {
    return ModuleRootManager.getInstance(getModule(moduleName)).sdk
  }


  fun setProjectLanguageLevel(languageLevel: LanguageLevel) {
    val languageLevelProjectExtension = LanguageLevelProjectExtension.getInstance(myProject)
    languageLevelProjectExtension.languageLevel = languageLevel
  }

  fun setProjectTargetBytecodeVersion(targetBytecodeVersion: String) {
    val compilerConfiguration = CompilerConfiguration.getInstance(myProject)
    compilerConfiguration.projectBytecodeTarget = targetBytecodeVersion
  }

  fun assertLanguageLevels(languageLevel: LanguageLevel, vararg moduleNames: String) {
    assertProjectLanguageLevel(languageLevel)
    for (moduleName in moduleNames) {
      assertModuleLanguageLevel(moduleName, languageLevel)
    }
  }

  fun assertTargetBytecodeVersions(targetBytecodeVersion: String, vararg moduleNames: String) {
    assertProjectTargetBytecodeVersion(targetBytecodeVersion)
    for (moduleName in moduleNames) {
      assertModuleTargetBytecodeVersion(moduleName, targetBytecodeVersion)
    }
  }

  fun assertProjectLanguageLevel(languageLevel: LanguageLevel) {
    assertEquals(languageLevel, getLanguageLevelForProject(myProject))
  }

  fun assertModuleLanguageLevel(moduleName: String, languageLevel: LanguageLevel) {
    assertEquals(languageLevel, getLanguageLevelForModule(moduleName))
  }

  fun assertProjectTargetBytecodeVersion(version: String) {
    val targetBytecodeVersion = getBytecodeTargetLevelForProject()
    assertEquals(version, targetBytecodeVersion)
  }

  fun assertModuleTargetBytecodeVersion(moduleName: String, version: String) {
    val targetBytecodeVersion = getBytecodeTargetLevelForModule(moduleName)
    assertEquals(version, targetBytecodeVersion)
  }
}