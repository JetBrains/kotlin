/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.debug.clientserver

import java.io.File
import java.io.ObjectOutputStream
import java.net.Socket
import java.net.URL

class TestProxy(
    private val serverPort: Int,
    private val testClass: String,
    private val methodName: String,
    private val classPath: List<URL>
) {
    fun runTest() {
        Socket("localhost", serverPort).use { clientSocket ->
            val output = ObjectOutputStream(clientSocket.getOutputStream())
            try {
                output.writeObject(MessageHeader.NEW_TEST)
                output.writeObject(testClass)
                output.writeObject(methodName)
                output.writeObject(MessageHeader.CLASS_PATH)
                // filter out jdk libs
                output.writeObject(filterOutJdkJars(classPath).toTypedArray())
            } finally {
                output.close()
            }
        }
    }

    private fun filterOutJdkJars(classPath: List<URL>): List<URL> {
        val javaHome = System.getProperty("java.home") ?: error("java.home is not")
        val javaFolder = File(javaHome)
        return classPath.filterNot {
            File(it.file).startsWith(javaFolder)
        }
    }
}