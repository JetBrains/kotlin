/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.utils

import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

const val ideaDebuggerDispatchPortKey = "idea.debugger.dispatch.port"

val ideaDebuggerDispatchPort: Int? by lazy {
    System.getProperty(ideaDebuggerDispatchPortKey)?.toInt()
}

fun issueNewDebugSessionJvmArguments(
    processName: String, intellijDebugDispatchPort: Int? = ideaDebuggerDispatchPort,
): Array<String> {
    intellijDebugDispatchPort?.let { dispatchPort ->
        val port = ServerSocket(0).use { it.localPort }
        Socket("127.0.0.1", dispatchPort).use { debugDispatchSocket ->
            val output = DataOutputStream(debugDispatchSocket.getOutputStream())
            output.use {
                output.writeUTF("Gradle JVM") // Debugger ID
                output.writeUTF(processName) // Process Name
                output.writeUTF("DEBUG_SERVER_PORT=$port") // Arguments
                output.flush()

                // wait for any response!
                debugDispatchSocket.inputStream.read()
            }
        }
        return arrayOf("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=$port")
    }
    return arrayOf()
}
