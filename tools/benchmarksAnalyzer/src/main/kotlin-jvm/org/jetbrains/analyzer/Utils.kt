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

import java.io.File
import java.io.InputStream

actual fun readFile(fileName: String): String {
    val inputStream = File(fileName).inputStream()
    val inputString = inputStream.bufferedReader().use { it.readText() }
    return inputString
}

actual fun format(number: Double, decimalNumber: Int): String =
    "%.${decimalNumber}f".format(number)

actual fun writeToFile(fileName: String, text: String) {
    File(fileName).printWriter().use { out ->
        out.println(text)
    }
}

actual fun assert(value: Boolean, lazyMessage: () -> Any) {
    kotlin.assert(value, lazyMessage)
}

actual fun getEnv(variableName:String): String? =
        System.getenv(variableName)
