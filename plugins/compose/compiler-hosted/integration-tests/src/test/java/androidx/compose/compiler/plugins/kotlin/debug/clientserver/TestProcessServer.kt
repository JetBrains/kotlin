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

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.Method
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private fun ClassLoader.loadClassOrNull(name: String): Class<*>? =
    try {
        loadClass(name)
    } catch (e: ClassNotFoundException) {
        null
    }

private fun Class<*>.getMethodOrNull(name: String, vararg parameterTypes: Class<*>): Method? =
    try {
        getMethod(name, *parameterTypes)
    } catch (e: NoSuchMethodException) {
        null
    }

private fun getGeneratedClass(classLoader: ClassLoader, className: String): Class<*> =
    classLoader.loadClassOrNull(className) ?: error("No class file was generated for: $className")

object TestProcessServer {

    const val DEBUG_TEST = "--debugTest"

    private val executor = Executors.newFixedThreadPool(1)!!

    @Volatile
    private var isProcessingTask = true

    @Volatile
    private var lastTime = System.currentTimeMillis()

    private val scheduler = Executors.newScheduledThreadPool(1)

    private lateinit var handler: ScheduledFuture<*>

    private lateinit var serverSocket: ServerSocket

    private var suppressOutput = false
    private var allocatePort = false

    @JvmStatic
    fun main(args: Array<String>) {
        if (args[0] == DEBUG_TEST) {
            suppressOutput = true
            allocatePort = true
        }

        val portNumber = if (allocatePort) 0 else args[0].toInt()
        println("Starting server on port $portNumber...")
        val serverSocket = ServerSocket(portNumber)
        println("...server started on port ${serverSocket.localPort}")
        scheduleShutdownProcess()

        try {
            while (true) {
                lastTime = System.currentTimeMillis()
                isProcessingTask = false
                val clientSocket = serverSocket.accept()
                isProcessingTask = true
                println("Socket established...")
                executor.execute(ServerTest(clientSocket, suppressOutput))
            }
        } finally {
            handler.cancel(false)
            scheduler.shutdown()
            serverSocket.close()
            println("Server stopped!")
        }
    }

    private fun scheduleShutdownProcess() {
        handler = scheduler.scheduleAtFixedRate(
            {
                if (!isProcessingTask && (System.currentTimeMillis() - lastTime) >= 60 * 1000) {
                    println("Stopping server...")
                    serverSocket.close()
                }
            },
            60, 60, TimeUnit.SECONDS
        )
    }
}

private class ServerTest(val clientSocket: Socket, val suppressOutput: Boolean) : Runnable {
    private lateinit var className: String
    private lateinit var testMethod: String

    override fun run() {
        val input = ObjectInputStream(clientSocket.getInputStream())
        val output =
            if (suppressOutput) null else ObjectOutputStream(clientSocket.getOutputStream())
        try {
            var message = input.readObject() as MessageHeader
            assert(message == MessageHeader.NEW_TEST) {
                "New test marker missed, but $message received"
            }
            className = input.readObject() as String
            testMethod = input.readObject() as String
            println("Preparing to execute test $className")

            message = input.readObject() as MessageHeader
            assert(message == MessageHeader.CLASS_PATH) {
                "Class path marker missed, but $message received"
            }
            @Suppress("UNCHECKED_CAST")
            val classPath = input.readObject() as Array<URL>

            executeTest(URLClassLoader(classPath))
            output?.writeObject(MessageHeader.RESULT)
        } catch (e: Throwable) {
            output?.writeObject(MessageHeader.ERROR)
            output?.writeObject(e)
        } finally {
            output?.close()
            input.close()
            clientSocket.close()
        }
    }

    fun executeTest(classLoader: ClassLoader) {
        val clazz = getGeneratedClass(classLoader, className)
        clazz.getMethodOrNull(testMethod)!!.invoke(null)
    }
}

enum class MessageHeader {
    NEW_TEST,
    CLASS_PATH,
    RESULT,
    ERROR
}