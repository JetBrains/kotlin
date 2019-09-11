// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.sender

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.service
import com.intellij.stats.network.assertNotEDT
import com.intellij.stats.network.service.RequestService
import com.intellij.stats.storage.FilePathProvider
import java.io.File

class StatisticSenderImpl: StatisticSender {
    companion object {
        const val DAILY_LIMIT = 15 * 1024 * 1024 // 15 mb
    }

    private val limitWatcher = DailyLimitSendingWatcher(DAILY_LIMIT, PersistentSentDataInfo(PropertiesComponent.getInstance()))

    override fun sendStatsData(url: String) {
        assertNotEDT()
        if (limitWatcher.isLimitReached()) return
        val filesToSend = service<FilePathProvider>().getDataFiles()
        filesToSend.forEach {
            if (it.length() > 0 && !limitWatcher.isLimitReached()) {
                val isSentSuccessfully = sendContent(url, it)
                if (isSentSuccessfully) {
                    it.delete()
                }
                else {
                    return
                }
            }
        }
    }

    private fun sendContent(url: String, file: File): Boolean {
        val data = service<RequestService>().postZipped(url, file)
        if (data != null && data.code >= 200 && data.code < 300) {
            if (data.sentDataSize != null) {
                limitWatcher.dataSent(data.sentDataSize)
            }
            return true
        }
        return false
    }

}