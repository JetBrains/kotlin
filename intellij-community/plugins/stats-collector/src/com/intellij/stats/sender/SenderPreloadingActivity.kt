// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.sender

import com.intellij.completion.settings.CompletionMLRankingSettings
import com.intellij.internal.statistic.utils.StatisticsUploadAssistant
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.stats.experiment.WebServiceStatus
import com.intellij.util.Alarm
import com.intellij.util.Time

private fun isSendAllowed(): Boolean {
  return StatisticsUploadAssistant.isSendAllowed() && CompletionMLRankingSettings.getInstance().isCompletionLogsSendAllowed
}

internal class SenderPreloadingActivity : PreloadingActivity() {
  private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, ApplicationManager.getApplication())
  private val sendInterval = 5 * Time.MINUTE

  override fun preload(indicator: ProgressIndicator) {
    val app = ApplicationManager.getApplication()
    if (app.isUnitTestMode || app.isHeadlessEnvironment) {
      return
    }

    if (isSendAllowed()) {
      alarm.addRequest({ send() }, sendInterval)
    }
  }

  private fun send() {
    if (!isSendAllowed()) {
      return
    }

    try {
      val statusHelper = WebServiceStatus.getInstance()
      statusHelper.updateStatus()
      if (statusHelper.isServerOk()) {
        service<StatisticSender>().sendStatsData(statusHelper.dataServerUrl())
      }
    }
    finally {
      alarm.addRequest({ send() }, sendInterval)
    }
  }
}