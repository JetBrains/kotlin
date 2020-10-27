/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.analyzer

import org.w3c.xhr.*
import kotlin.browser.*
import kotlin.js.*

actual fun readFile(fileName: String): String {
    error("Reading from local file for JS isn't supported")
}

actual fun writeToFile(fileName: String, text: String) {
    error("Writing to local file for JS isn't supported")
}

actual fun Double.format(decimalNumber: Int): String =
        this.asDynamic().toFixed(decimalNumber)

actual fun assert(value: Boolean, lazyMessage: () -> Any) {
    if (!value) error(lazyMessage)
}

actual fun sendGetRequest(url: String, user: String?, password: String?, followLocation: Boolean) : String {
    error("Unsupported")
}