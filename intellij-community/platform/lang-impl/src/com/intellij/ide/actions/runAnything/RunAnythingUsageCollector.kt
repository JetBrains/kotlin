// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything

import com.intellij.ide.actions.runAnything.RunAnythingAction.RUN_ANYTHING
import com.intellij.ide.actions.runAnything.groups.RunAnythingGroup
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.internal.statistic.utils.getPluginInfo
import com.intellij.openapi.project.Project

private const val GROUP_ID = "actions.runAnything"

class RunAnythingUsageCollector {
  companion object {
    private val allowedCompletionGroupTitles: Array<String> = arrayOf("Bundler", "rails generators", "Gradle tasks", "npm scripts",
                                                                      "Maven goals",
                                                                      "rvm use", "rake", "rbenv shell", "General", "Recent projects",
                                                                      "Run configurations")

    private val allowedHelpGroupTitles: Array<String> = arrayOf("Gradle", "Maven", "npm", "Python", "Recent projects", "ruby", "General",
                                                                "Recent")

    fun trigger(project: Project, featureId: String) {
      FUCounterUsageLogger.getInstance().logEvent(project, GROUP_ID, featureId)
    }

    fun triggerExecCategoryStatistics(project: Project,
                                      groups: Collection<RunAnythingGroup>,
                                      clazz: Class<out RunAnythingSearchListModel>,
                                      index: Int) {
      for (i in index downTo 0) {
        val group = RunAnythingGroup.findGroup(groups, i)
        if (group != null) {
          RunAnythingUsageCollector.trigger(project, getSafeToReportClazzName(clazz) + ": " +
                                                     RUN_ANYTHING + " - execution - " + getSafeToReportTitle(group))
          break
        }
      }
    }

    fun triggerMoreStatistics(project: Project,
                              group: RunAnythingGroup,
                              clazz: Class<out RunAnythingSearchListModel>) {
      RunAnythingUsageCollector.trigger(project, getSafeToReportClazzName(clazz) + ": " +
                                                 RUN_ANYTHING + " - more - " + getSafeToReportTitle(group))
    }

    private fun getSafeToReportClazzName(clazz: Class<*>): String {
      return if (getPluginInfo(clazz).isSafeToReport()) clazz.simpleName else "third.party"
    }

    private fun getSafeToReportTitle(group: RunAnythingGroup): String {
      return if (!getPluginInfo(group.javaClass).isSafeToReport()) "third.party"
      else return if (group.title in allowedCompletionGroupTitles || group.title in allowedHelpGroupTitles) group.title else "run.anything.group.unknown.title"
    }
  }
}