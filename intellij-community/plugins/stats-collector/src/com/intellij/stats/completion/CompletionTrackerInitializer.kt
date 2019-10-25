// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener

class CompletionTrackerInitializer : ApplicationInitializedListener {
  companion object {
    var isEnabledInTests: Boolean = false
  }

  override fun componentsInitialized() {
    val busConnection = ApplicationManager.getApplication().messageBus.connect()
    if (CompletionLoggerInitializer.shouldInitialize()) {
      val actionListener = LookupActionsListener()
      busConnection.subscribe(AnActionListener.TOPIC, actionListener)
      busConnection.subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
        override fun projectOpened(project: Project) {
          LookupManager.getInstance(project).addPropertyChangeListener(CompletionLoggerInitializer(actionListener), project)
        }
      })
    }

    busConnection.subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
      override fun projectOpened(project: Project) {
        LookupManager.getInstance(project).addPropertyChangeListener(CompletionQualityTracker(), project)
        LookupManager.getInstance(project).addPropertyChangeListener(CompletionFactorsInitializer(), project)
      }
    })
  }
}