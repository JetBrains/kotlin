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
package com.intellij.stats.completion

import com.intellij.stats.network.service.RequestService
import com.intellij.stats.network.service.ResponseData
import com.intellij.stats.sender.StatisticSenderImpl
import com.intellij.stats.storage.FilePathProvider
import com.intellij.testFramework.LightPlatformTestCase
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
        
        val sender = StatisticSenderImpl(requestService, filePathProvider)
        sender.sendStatsData(testUrl)
        
        assertThat(firstFile.exists()).isEqualTo(false)
        assertThat(secondFile.exists()).isEqualTo(false)
    }


    fun `test removed first if only first is sent`() {
        val requestService = mock(RequestService::class.java).apply {
            `when`(postZipped(testUrl, firstFile)).thenReturn(okResponse())
            `when`(postZipped(testUrl, secondFile)).thenReturn(failResponse())
        }
        
        val sender = StatisticSenderImpl(requestService, filePathProvider)
        sender.sendStatsData(testUrl)

        assertThat(firstFile.exists()).isEqualTo(false)
        assertThat(secondFile.exists()).isEqualTo(true)
    }

    fun `test none is removed if all send failed`() {
        val requestService = mock(RequestService::class.java).apply {
            `when`(postZipped(testUrl, firstFile)).thenReturn(failResponse())
            `when`(postZipped(testUrl, secondFile)).thenThrow(IllegalStateException("Should not be invoked"))
        }

        val sender = StatisticSenderImpl(requestService, filePathProvider)
        sender.sendStatsData(testUrl)

        assertThat(firstFile.exists()).isEqualTo(true)
        assertThat(secondFile.exists()).isEqualTo(true)
    }
    
}


fun okResponse(message: String = "") = ResponseData(200, message)
fun failResponse(message: String = "") = ResponseData(404, message)