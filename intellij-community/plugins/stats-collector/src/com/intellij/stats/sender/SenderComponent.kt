// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.sender

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.diagnostic.logger
import com.intellij.reporting.isSendAllowed
import com.intellij.stats.experiment.WebServiceStatus
import com.intellij.util.Alarm
import com.intellij.util.Time

class SenderComponent(private val sender: StatisticSender, private val statusHelper: WebServiceStatus) : BaseComponent, Disposable {
  private companion object {
    val LOG = logger<SenderComponent>()
  }

  private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
  private val sendInterval = 5 * Time.MINUTE

  private fun send() {
    if (ApplicationManager.getApplication().isUnitTestMode || ApplicationManager.getApplication().isHeadlessEnvironment) return

    try {
      ApplicationManager.getApplication().executeOnPooledThread {
        if(!isSendAllowed()) return@executeOnPooledThread
        statusHelper.updateStatus()
        if (statusHelper.isServerOk()) {
          val dataServerUrl = statusHelper.dataServerUrl()
          sender.sendStatsData(dataServerUrl)
        }
      }
    }
    catch (e: Exception) {
      LOG.error(e.message)
    }
    finally {
      alarm.addRequest({ send() }, sendInterval)
    }
  }

  override fun initComponent() {
    if (isSendAllowed()) {
      ApplicationManager.getApplication().executeOnPooledThread {
        send()
      }
    }
  }

  override fun dispose() {
  }
}