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
package com.intellij.stats.experiment

import com.intellij.stats.network.service.RequestService
import com.intellij.stats.network.service.ResponseData
import com.intellij.testFramework.LightIdeaTestCase
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class StatusInfoProviderTest : LightIdeaTestCase() {


    private fun newResponse(status: String, salt: String, version: String, url: String) = """
{
  "status" : "$status",
  "salt" : "$salt",
  "experimentVersion" : $version,
  "urlForZipBase64Content": "$url"  
}
"""
    
    fun `test experiment info is fetched`() {
        val response = newResponse("ok", "sdfs", "2", "http://test.jetstat-resty.aws.intellij.net/uploadstats")
        val infoProvider = getProvider(response)

        infoProvider.updateStatus()

        assertThat(infoProvider.dataServerUrl()).isEqualTo("http://test.jetstat-resty.aws.intellij.net/uploadstats")
        assertThat(infoProvider.isServerOk()).isEqualTo(true)
        assertThat(infoProvider.experimentVersion()).isEqualTo(2)
    }

    fun `test server is not ok`() {
        val response = newResponse("maintance", "sdfs", "2", "http://xxx.xxx")
        val infoProvider = getProvider(response)

        infoProvider.updateStatus()

        assertThat(infoProvider.isServerOk()).isEqualTo(false)
        assertThat(infoProvider.experimentVersion()).isEqualTo(2)
        assertThat(infoProvider.dataServerUrl()).isEqualTo("http://xxx.xxx")
    }

    fun `test round to Int`() {
        var response = newResponse("maintance", "sdfs", "2.9", "http://xxx.xxx")
        var infoProvider = getProvider(response)
        infoProvider.updateStatus()
        assertThat(infoProvider.experimentVersion()).isEqualTo(2)
        
        response = newResponse("maintance", "sdfs", "2.1", "http://xxx.xxx")
        infoProvider = getProvider(response)
        infoProvider.updateStatus()
        assertThat(infoProvider.experimentVersion()).isEqualTo(2)
    }


    private fun getProvider(response: String): WebServiceStatus {
        val requestSender = mock(RequestService::class.java).apply {
            `when`(get(ArgumentMatchers.anyString())).thenReturn(ResponseData(200, response))
        }
        return WebServiceStatusProvider(requestSender)
    }

}