/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.analyzer

import org.w3c.xhr.*
import kotlin.browser.*
import kotlin.js.*

actual fun readFile(fileName: String): String {
    error("Reading from local file for JS isn't supported")
}

actual fun Double.format(decimalNumber: Int): String =
        this.asDynamic().toFixed(decimalNumber)

actual fun writeToFile(fileName: String, text: String) {
    if (fileName != "html")
        error("Writing to local file for JS isn't supported")
    val bodyPart = text.substringAfter("<body>").substringBefore("</body>")
    document.body?.innerHTML = bodyPart
}

actual fun assert(value: Boolean, lazyMessage: () -> Any) {
    if (!value) error(lazyMessage)
}

actual fun sendGetRequest(url: String, user: String?, password: String?, followLocation: Boolean) : String {
    val proxyServerAddress = "https://perf-proxy.labs.jb.gg/"
    val newUrl = proxyServerAddress + url
    val request = XMLHttpRequest()

    request.open("GET", newUrl, false, user, password)
    request.send()
    if (request.status == 200.toShort()) {
        return request.responseText
    }
    error("Request to $url has status ${request.status}")
}