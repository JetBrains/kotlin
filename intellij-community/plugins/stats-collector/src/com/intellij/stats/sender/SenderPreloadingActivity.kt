// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.sender

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.reporting.isSendAllowed
import com.intellij.stats.experiment.WebServiceStatus
import com.intellij.util.Alarm
import com.intellij.util.Time

internal class SenderPreloadingActivity : PreloadingActivity() {
  private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, ApplicationManager.getApplication())
  private val sendInterval = 5 * Time.MINUTE

  override fun preload(indicator: ProgressIndicator) {
    if (isSendAllowed()) {
      send()
    }
  }

  private fun send() {
    if (ApplicationManager.getApplication().isUnitTestMode || ApplicationManager.getApplication().isHeadlessEnvironment) {
      return
    }

    try {
      ApplicationManager.getApplication().executeOnPooledThread {
        if (!isSendAllowed()) {
          return@executeOnPooledThread
        }

        val statusHelper = WebServiceStatus.getInstance()
        statusHelper.updateStatus()
        if (statusHelper.isServerOk()) {
          val dataServerUrl = statusHelper.dataServerUrl()
          service<StatisticSender>().sendStatsData(dataServerUrl)
        }
      }
    }
    finally {
      alarm.addRequest({ send() }, sendInterval)
    }
  }
}