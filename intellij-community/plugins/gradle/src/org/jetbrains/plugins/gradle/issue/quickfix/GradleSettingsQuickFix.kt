// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue.quickfix

import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ex.ConfigurableVisitor
import com.intellij.openapi.options.newEditor.SettingsDialogFactory
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.issue.quickfix.ReimportQuickFix.Companion.requestImport
import org.jetbrains.plugins.gradle.service.settings.GradleConfigurable
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.util.concurrent.CompletableFuture
import java.util.function.BiPredicate

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class GradleSettingsQuickFix(private val myProjectPath: String, private val myRequestImport: Boolean,
                             private val myConfigurationChangeDetector: BiPredicate<GradleProjectSettings, GradleProjectSettings>?,
                             private val myFilter: String?) : BuildIssueQuickFix {

  override val id: String = "fix_gradle_settings"

  override fun runQuickFix(project: Project, dataProvider: DataProvider): CompletableFuture<*> {
    val projectSettings = GradleSettings.getInstance(project).getLinkedProjectSettings(myProjectPath)
                          ?: return CompletableFuture.completedFuture(false)
    val future = CompletableFuture<Boolean>()
    ApplicationManager.getApplication().invokeLater {
      val oldSettings: GradleProjectSettings?
      oldSettings = projectSettings.clone()

      val groups = ShowSettingsUtilImpl.getConfigurableGroups(project, true)
      val configurable = ConfigurableVisitor.ByType(GradleConfigurable::class.java).find(*groups)
      val dialogWrapper = SettingsDialogFactory.getInstance().create(project, groups, configurable, myFilter)
      val result = dialogWrapper.showAndGet()
      future.complete(result && myConfigurationChangeDetector != null && myConfigurationChangeDetector.test(oldSettings, projectSettings))
    }
    return future.thenCompose { isSettingsChanged ->
      if (isSettingsChanged!! && myRequestImport)
        requestImport(project, myProjectPath)
      else
        CompletableFuture.completedFuture(null)
    }
  }
}
