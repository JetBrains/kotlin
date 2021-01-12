// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
    return object : WebServiceStatusProvider() {
      override fun getRequestService() = requestSender
    }
  }
}