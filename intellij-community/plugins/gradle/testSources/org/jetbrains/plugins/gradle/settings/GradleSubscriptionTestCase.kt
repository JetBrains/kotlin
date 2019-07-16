// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.importing.GradleBuildScriptBuilderEx
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.junit.runners.Parameterized

abstract class GradleSubscriptionTestCase : GradleImportingTestCase() {

  protected fun linkProject() {
    importProject(GradleBuildScriptBuilderEx().withJavaPlugin().generate())
  }

  protected fun unlinkProject() {
    val settings = ExternalSystemApiUtil.getSettings(myProject, externalSystemId)
    settings.unlinkExternalProject(currentExternalProjectSettings.externalProjectPath)
  }

  protected fun onProjectLinked(subscription: Disposable, listener: () -> Unit) {
    val settingsListener = object : GradleSettingsListenerAdapter() {
      override fun onProjectsLinked(settings: MutableCollection<GradleProjectSettings>) = listener()
    }
    ExternalSystemApiUtil.subscribe(myProject, externalSystemId, subscription, settingsListener)
  }

  protected fun onProjectLinked(listener: () -> Unit) {
    val settingsListener = object : GradleSettingsListenerAdapter() {
      override fun onProjectsLinked(settings: MutableCollection<GradleProjectSettings>) = listener()
    }
    ExternalSystemApiUtil.subscribe(myProject, externalSystemId, settingsListener)
  }

  companion object {
    /**
     * It's sufficient to run the test against one gradle version
     */
    @Parameterized.Parameters(name = "with Gradle-{0}")
    @JvmStatic
    fun tests(): Collection<Array<out String>> = arrayListOf(arrayOf(BASE_GRADLE_VERSION))
  }
}