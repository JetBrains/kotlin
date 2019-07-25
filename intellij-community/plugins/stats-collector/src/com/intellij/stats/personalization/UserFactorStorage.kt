/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.stats.personalization

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.stats.personalization.impl.ApplicationUserFactorStorage
import com.intellij.stats.personalization.impl.ProjectUserFactorStorage

/**
 * @author Vitaliy.Bibaev
 */
interface UserFactorStorage {
  companion object {
    fun getInstance(): UserFactorStorage =
        ApplicationManager.getApplication().getComponent(ApplicationUserFactorStorage::class.java)

    fun getInstance(project: Project): UserFactorStorage = project.getComponent(ProjectUserFactorStorage::class.java)

    fun <U : FactorUpdater> applyOnBoth(project: Project, description: UserFactorDescription<U, *>, updater: (U) -> Unit) {
      updater(getInstance().getFactorUpdater(description))
      updater(getInstance(project).getFactorUpdater(description))
    }
  }

  fun <U : FactorUpdater> getFactorUpdater(description: UserFactorDescription<U, *>): U
  fun <R : FactorReader> getFactorReader(description: UserFactorDescription<*, R>): R
}