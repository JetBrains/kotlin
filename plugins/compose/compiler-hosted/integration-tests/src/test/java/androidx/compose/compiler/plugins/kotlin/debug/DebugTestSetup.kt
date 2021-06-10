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

package androidx.compose.compiler.plugins.kotlin.debug

import androidx.compose.compiler.plugins.kotlin.debug.clientserver.TestProcessServer
import com.intellij.util.PathUtil
import com.intellij.util.SystemProperties
import com.sun.jdi.VirtualMachine
import com.sun.tools.jdi.SocketAttachingConnector
import junit.extensions.TestSetup
import junit.framework.Test
import java.io.File
import kotlin.properties.Delegates

/**
 * An utility that allows sharing of the [TestProcessServer] and debugger across multiple tests.
 * It startups [TestProcessServer] and attaches debugger to it.
 */
class DebugTestSetup(
    test: Test,
    val onDebugEnvironmentAvailable: (DebugEnvironment) -> Unit
) : TestSetup(test) {
    private lateinit var testServerProcess: Process

    override fun setUp() {
        super.setUp()
        testServerProcess = startTestProcessServer()
        val (debuggerPort, proxyPort) = testServerProcess.inputStream.bufferedReader().use {
            val debuggerPort = it.readLine().split("address:").last().trim().toInt()
            it.readLine()
            val proxyPort = it.readLine().split("port ").last().trim().toInt()
            (debuggerPort to proxyPort)
        }
        val virtualMachine = attachDebugger(debuggerPort)
        onDebugEnvironmentAvailable(DebugEnvironment(virtualMachine, proxyPort))
    }

    override fun tearDown() {
        super.tearDown()
        testServerProcess.destroy()
    }
}

class DebugEnvironment(val virtualMachine: VirtualMachine, val proxyPort: Int)

private fun startTestProcessServer(): Process {
    val classpath = listOf(
        PathUtil.getJarPathForClass(TestProcessServer::class.java),
        PathUtil.getJarPathForClass(Delegates::class.java) // Add Kotlin runtime JAR
    )

    val javaExec = File(File(SystemProperties.getJavaHome(), "bin"), "java")
    assert(javaExec.exists())

    val command = listOf(
        javaExec.absolutePath,
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:0",
        "-ea",
        "-classpath", classpath.joinToString(File.pathSeparator),
        TestProcessServer::class.qualifiedName,
        TestProcessServer.DEBUG_TEST
    )

    return ProcessBuilder(command).start()
}

private const val DEBUG_ADDRESS = "127.0.0.1"

private fun attachDebugger(port: Int): VirtualMachine {
    val connector = SocketAttachingConnector()
    return connector.attach(
        connector.defaultArguments().apply {
            getValue("port").setValue("$port")
            getValue("hostname").setValue(DEBUG_ADDRESS)
        }
    )
}