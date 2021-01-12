// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl

import com.intellij.ProjectTopics
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver
import com.intellij.openapi.startup.StartupActivity

internal class UnknownSdkStartupChecker : StartupActivity.DumbAware {
  override fun runActivity(project: Project) {
    checkUnknownSdks(project)

    UnknownSdkResolver.EP_NAME.addExtensionPointListener(object: ExtensionPointListener<UnknownSdkResolver> {
      override fun extensionAdded(extension: UnknownSdkResolver, pluginDescriptor: PluginDescriptor) {
        checkUnknownSdks(project)
      }

      override fun extensionRemoved(extension: UnknownSdkResolver, pluginDescriptor: PluginDescriptor) {
        checkUnknownSdks(project)
      }
    }, project)

    project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object: ModuleRootListener {
      override fun rootsChanged(event: ModuleRootEvent) {
        checkUnknownSdks(event.project)
      }
    })

    ProjectRootManagerEx.getInstanceEx(project).addProjectJdkListener {
      checkUnknownSdks(project)
    }

    project.messageBus.connect().subscribe(ProjectJdkTable.JDK_TABLE_TOPIC, object : ProjectJdkTable.Listener {
      override fun jdkAdded(jdk: Sdk) {
        checkUnknownSdks(project)
      }

      override fun jdkRemoved(jdk: Sdk) {
        checkUnknownSdks(project)
      }

      override fun jdkNameChanged(jdk: Sdk, previousName: String) {
        checkUnknownSdks(project)
      }
    })
  }

  private fun checkUnknownSdks(project: Project) {
    if (project.isDisposed || project.isDefault) return
    //TODO: workaround for tests, right not it can happen that project.earlyDisposable is null with @NotNull annotation
    if (project is ProjectEx && kotlin.runCatching { project.earlyDisposable }.isFailure) return
    UnknownSdkTracker.getInstance(project).updateUnknownSdks()
  }
}
