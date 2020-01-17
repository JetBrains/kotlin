// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.test.ExternalSystemTestCase
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.util.GradleConstants

abstract class GradleSubscriptionTestCase : ExternalSystemTestCase() {

  private var _projectSettings: GradleProjectSettings? = null

  private val projectSettings get() = _projectSettings!!

  private val externalSystemId = GradleConstants.SYSTEM_ID

  override fun getTestsTempDir() = "tmp"

  override fun getExternalSystemConfigFileName() = "build.gradle"

  override fun setUp() {
    super.setUp()
    _projectSettings = GradleProjectSettings()
    projectSettings.externalProjectPath = projectPath
  }

  override fun tearDown() {
    _projectSettings = null
    super.tearDown()
  }

  protected fun linkProject() {
    val settings = ExternalSystemApiUtil.getSettings(myProject, externalSystemId)
    projectSettings.externalProjectPath = projectPath
    settings.linkProject(projectSettings)
  }

  protected fun unlinkProject() {
    val settings = ExternalSystemApiUtil.getSettings(myProject, externalSystemId)
    settings.unlinkExternalProject(projectSettings.externalProjectPath)
  }

  protected fun onProjectLinked(subscription: Disposable, listener: () -> Unit) {
    val settingsListener = object : GradleSettingsListenerAdapter() {
      override fun onProjectsLinked(settings: MutableCollection<GradleProjectSettings>) = listener()
    }
    ExternalSystemApiUtil.subscribe(myProject, externalSystemId, settingsListener, subscription)
  }

  protected fun onProjectLinked(listener: () -> Unit) {
    val settingsListener = object : GradleSettingsListenerAdapter() {
      override fun onProjectsLinked(settings: MutableCollection<GradleProjectSettings>) = listener()
    }
    ExternalSystemApiUtil.subscribe(myProject, externalSystemId, settingsListener)
  }
}