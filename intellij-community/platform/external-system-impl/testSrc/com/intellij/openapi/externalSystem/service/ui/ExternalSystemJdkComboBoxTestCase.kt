// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.getJavaSdkType
import com.intellij.openapi.externalSystem.service.ui.ExternalSystemJdkComboBox.select
import com.intellij.openapi.externalSystem.test.ExternalSystemTestCase
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import java.io.File
import java.util.*

@Suppress("PropertyName")
abstract class ExternalSystemJdkComboBoxTestCase : ExternalSystemTestCase() {
  override fun getTestsTempDir() = "tmp"
  override fun getExternalSystemConfigFileName() = null

  protected val JDK6 by lazy { createFakeJdk("6", "1.6") }
  protected val JDK7 by lazy { createFakeJdk("7", "1.7") }
  protected val JDK8 by lazy { createFakeJdk("8", "1.8") }
  protected val JDK9 by lazy { createFakeJdk("9", "9") }
  protected val JDK10 by lazy { createFakeJdk("10", "10") }
  protected val JDK11 by lazy { createFakeJdk("11", "11") }

  protected val comboBox by lazy {
    invokeAndWaitIfNeeded {
      runWriteAction {
        val jdkTable = ProjectJdkTable.getInstance()
        jdkTable.addJdk(JDK6, myProject)
        jdkTable.addJdk(JDK7, myProject)
        jdkTable.addJdk(JDK8, myProject)
        jdkTable.addJdk(JDK9, myProject)
        jdkTable.addJdk(JDK10, myProject)
        jdkTable.addJdk(JDK11, myProject)
      }
    }
    ExternalSystemJdkComboBox()
  }

  protected fun assertSelectedJdk(selectionName: String, jdk: Sdk) {
    select(comboBox.model, selectionName)
    val selectedJdk = comboBox.selectedJdk!!
    assertEquals(jdk.name, selectedJdk.name)
    assertEquals(jdk.versionString, selectedJdk.versionString)
    assertEquals(jdk.homePath, selectedJdk.homePath)
  }

  /**
   * Creates fake jdk with specified version
   * @see com.intellij.openapi.projectRoots.JdkUtil.checkForJdk
   */
  private fun createFakeJdk(name: String, version: String): Sdk {
    val sdk = ProjectJdkImpl(name, getJavaSdkType())
    sdk.homePath = "$projectPath/jdk-$name"
    createProjectSubFile("jdk-$name/release")
    createProjectSubFile("jdk-$name/jre/lib/rt.jar")
    createProjectSubFile("jdk-$name/bin/javac")
    createProjectSubFile("jdk-$name/bin/java")
    val properties = Properties()
    properties.setProperty("JAVA_FULL_VERSION", version)
    File("$projectPath/jdk-$name/release").outputStream().use {
      properties.store(it, null)
    }
    return sdk
  }
}
