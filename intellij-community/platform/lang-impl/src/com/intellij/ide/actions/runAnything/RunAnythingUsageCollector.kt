// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything

import com.intellij.ide.actions.runAnything.groups.RunAnythingGroup
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.internal.statistic.utils.getPluginInfo
import com.intellij.openapi.project.Project

private const val GROUP_ID = "actions.runAnything"

class RunAnythingUsageCollector {
  companion object {
    fun trigger(project: Project, featureId: String) {
      FUCounterUsageLogger.getInstance().logEvent(project, GROUP_ID, featureId)
    }

    fun triggerExecCategoryStatistics(project: Project,
                                      groups: MutableCollection<out RunAnythingGroup>,
                                      clazz: Class<out RunAnythingSearchListModel>,
                                      index: Int,
                                      shiftPressed: Boolean,
                                      altPressed: Boolean) {
      for (i in index downTo 0) {
        val group = RunAnythingGroup.findGroup(groups, i)
        if (group != null) {
          FUCounterUsageLogger.getInstance().logEvent(project, GROUP_ID, "execute", FeatureUsageData()
            .addData("list", getSafeToReportClazzName(clazz))
            .addData("group", getSafeToReportTitle(group))
            .addData("with_shift", shiftPressed)
            .addData("with_alt", altPressed))
          break
        }
      }
    }

    fun triggerMoreStatistics(project: Project,
                              group: RunAnythingGroup,
                              clazz: Class<out RunAnythingSearchListModel>) {
      FUCounterUsageLogger.getInstance().logEvent(project, GROUP_ID, "click.more", FeatureUsageData()
        .addData("list", getSafeToReportClazzName(clazz))
        .addData("group", getSafeToReportTitle(group)))
    }


    private fun getSafeToReportClazzName(clazz: Class<*>): String {
      return if (getPluginInfo(clazz).isSafeToReport()) clazz.simpleName else "third.party"
    }

    private fun getSafeToReportTitle(group: RunAnythingGroup): String {
      return if (!getPluginInfo(group.javaClass).isSafeToReport()) "third.party"
      else return group.title
    }
  }
}