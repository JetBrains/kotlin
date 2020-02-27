// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.suggested

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.TestOnly

class SuggestedRefactoringProviderImpl(project: Project) : SuggestedRefactoringProvider {
  val availabilityIndicator = SuggestedRefactoringAvailabilityIndicator(project)
  private val changeCollector = SuggestedRefactoringChangeCollector(availabilityIndicator)
  private val listener = SuggestedRefactoringChangeListener(project, changeCollector)

  val state: SuggestedRefactoringState?
    get() = changeCollector.state

  internal class Startup : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
      val listener = getInstance(project).listener
      listener.attach()
      Disposer.register(project, listener)
    }
  }

  override fun reset() {
    listener.reset()
  }

  fun suppressForCurrentDeclaration() {
    listener.suppressForCurrentDeclaration()
  }

  @set:TestOnly
  var _amendStateInBackgroundEnabled: Boolean
    get() = changeCollector._amendStateInBackgroundEnabled
    set(value) { changeCollector._amendStateInBackgroundEnabled = value }

  companion object {
    fun getInstance(project: Project): SuggestedRefactoringProviderImpl =
      SuggestedRefactoringProvider.getInstance(project) as SuggestedRefactoringProviderImpl
  }
}
