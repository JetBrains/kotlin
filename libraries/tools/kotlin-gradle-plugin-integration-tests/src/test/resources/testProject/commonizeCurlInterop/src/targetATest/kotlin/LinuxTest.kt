/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.toKString
import curl.*

import kotlin.test.Test

@OptIn(ExperimentalUnsignedTypes::class)
class LinuxTest {
    @Test
    fun curl() {
        val curl = curl_easy_init()
        if (curl != null) {
            curl_easy_setopt(curl, CURLOPT_URL, "http://example.com")
            curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L)
            val res = curl_easy_perform(curl)
            if (res != CURLE_OK) {
                println("curl_easy_perform() failed ${curl_easy_strerror(res)?.toKString()}")
            }
            curl_easy_cleanup(curl)
        }
    }
}
