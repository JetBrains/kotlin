// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.openapi.application.ApplicationManager
import com.intellij.stats.network.service.RequestService
import com.intellij.stats.network.service.ResponseData
import com.intellij.stats.sender.StatisticSenderImpl
import com.intellij.stats.storage.FilePathProvider
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.replaceService
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

class StatisticsSenderTest: LightPlatformTestCase() {
    private lateinit var firstFile: File
    private lateinit var secondFile: File
    private lateinit var filePathProvider: FilePathProvider

    private val testUrl = "http://xxx.com"

    override fun setUp() {
        super.setUp()

        firstFile = File("first_file")
        firstFile.createNewFile()
        firstFile.writeText("text")

        secondFile = File("second_file")
        secondFile.createNewFile()
        secondFile.writeText("text")

        filePathProvider = mock(FilePathProvider::class.java).apply {
            `when`(getDataFiles()).thenReturn(listOf(firstFile, secondFile))
        }
    }

    override fun tearDown() {
        try {
            firstFile.delete()
            secondFile.delete()
        }
        finally {
            super.tearDown()
        }
    }

    fun `test removed if every file send response was ok`() {
        val requestService = mock(RequestService::class.java).apply {
            `when`(postZipped(testUrl, firstFile)).thenReturn(okResponse())
            `when`(postZipped(testUrl, secondFile)).thenReturn(okResponse())
        }

        val app = ApplicationManager.getApplication()
        app.replaceService(FilePathProvider::class.java, filePathProvider, testRootDisposable)
        app.replaceService(RequestService::class.java, requestService, testRootDisposable)

        val sender = StatisticSenderImpl()
        sender.sendStatsData(testUrl)

        assertThat(firstFile.exists()).isEqualTo(false)
        assertThat(secondFile.exists()).isEqualTo(false)
    }


    fun `test removed first if only first is sent`() {
        val requestService = mock(RequestService::class.java).apply {
            `when`(postZipped(testUrl, firstFile)).thenReturn(okResponse())
            `when`(postZipped(testUrl, secondFile)).thenReturn(failResponse())
        }

        val app = ApplicationManager.getApplication()
        app.replaceService(FilePathProvider::class.java, filePathProvider, testRootDisposable)
        app.replaceService(RequestService::class.java, requestService, testRootDisposable)

        val sender = StatisticSenderImpl()
        sender.sendStatsData(testUrl)

        assertThat(firstFile.exists()).isEqualTo(false)
        assertThat(secondFile.exists()).isEqualTo(true)
    }

    fun `test none is removed if all send failed`() {
        val requestService = mock(RequestService::class.java).apply {
            `when`(postZipped(testUrl, firstFile)).thenReturn(failResponse())
            `when`(postZipped(testUrl, secondFile)).thenThrow(IllegalStateException("Should not be invoked"))
        }

        val app = ApplicationManager.getApplication()
        app.replaceService(FilePathProvider::class.java, filePathProvider, testRootDisposable)
        app.replaceService(RequestService::class.java, requestService, testRootDisposable)

        val sender = StatisticSenderImpl()
        sender.sendStatsData(testUrl)

        assertThat(firstFile.exists()).isEqualTo(true)
        assertThat(secondFile.exists()).isEqualTo(true)
    }

}


fun okResponse(message: String = "") = ResponseData(200, message)
fun failResponse(message: String = "") = ResponseData(404, message)