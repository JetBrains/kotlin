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

import androidx.compose.compiler.plugins.kotlin.AbstractCodegenTest
import androidx.compose.compiler.plugins.kotlin.debug.clientserver.TestProcessServer
import androidx.compose.compiler.plugins.kotlin.debug.clientserver.TestProxy
import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import com.intellij.util.PathUtil
import com.intellij.util.SystemProperties
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.LocatableEvent
import com.sun.jdi.event.MethodEntryEvent
import com.sun.jdi.event.MethodExitEvent
import com.sun.jdi.event.StepEvent
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.event.VMStartEvent
import com.sun.jdi.request.EventRequest.SUSPEND_ALL
import com.sun.jdi.request.MethodEntryRequest
import com.sun.jdi.request.MethodExitRequest
import com.sun.jdi.request.StepRequest
import com.sun.tools.jdi.SocketAttachingConnector
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.properties.Delegates
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.TemporaryFolder

private const val RUNNER_CLASS = "RunnerKt"
private const val MAIN_METHOD = "main"
private const val CONTENT_METHOD = "content"
private const val TEST_CLASS = "TestKt"

abstract class AbstractDebuggerTest(useFir: Boolean) : AbstractCodegenTest(useFir) {
    companion object {
        private lateinit var testServerProcess: Process
        lateinit var virtualMachine: VirtualMachine
        var proxyPort: Int = -1

        @JvmStatic
        @BeforeClass
        fun startDebugProcess() {
            testServerProcess = startTestProcessServer()
            val (debuggerPort, _proxyPort) = testServerProcess.inputStream.bufferedReader().use {
                val debuggerPort = it.readLine().split("address:").last().trim().toInt()
                it.readLine()
                val proxyPort = it.readLine().split("port ").last().trim().toInt()
                (debuggerPort to proxyPort)
            }
            virtualMachine = attachDebugger(debuggerPort)
            proxyPort = _proxyPort
        }

        @JvmStatic
        @AfterClass
        fun stopDebugProcess() {
            testServerProcess.destroy()
        }

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
    }

    private lateinit var methodEntryRequest: MethodEntryRequest
    private lateinit var methodExitRequest: MethodExitRequest

    @Before
    fun createMethodEventsForTestClass() {
        val manager = virtualMachine.eventRequestManager()
        methodEntryRequest = manager.createMethodEntryRequest()
        methodEntryRequest.addClassFilter(TEST_CLASS)
        methodEntryRequest.setSuspendPolicy(SUSPEND_ALL)
        methodEntryRequest.enable()

        methodExitRequest = manager.createMethodExitRequest()
        methodExitRequest.addClassFilter(TEST_CLASS)
        methodExitRequest.setSuspendPolicy(SUSPEND_ALL)
        methodExitRequest.enable()
    }

    @After
    fun deleteEventRequests() {
        virtualMachine.eventRequestManager()
            .deleteEventRequests(listOf(methodEntryRequest, methodExitRequest))
    }

    @JvmField
    @Rule
    val outDirectory = TemporaryFolder()

    private fun invokeRunnerMainInSeparateProcess(
        classLoader: URLClassLoader,
        port: Int
    ) {
        val classPath = classLoader.extractUrls().toMutableList()
        if (classLoader is GeneratedClassLoader) {
            val outDir = outDirectory.root
            val currentOutput = SimpleOutputFileCollection(classLoader.allGeneratedFiles)
            currentOutput.writeAllTo(outDir)
            classPath.add(0, outDir.toURI().toURL())
        }
        TestProxy(port, RUNNER_CLASS, MAIN_METHOD, classPath).runTest()
    }

    fun collectDebugEvents(@Language("kotlin") source: String): List<LocatableEvent> {
        val classLoader = createClassLoader(
            listOf(
                SourceFile("Runner.kt", RUNNER_SOURCES),
                SourceFile("Test.kt", source)
            )
        )
        val testClass = classLoader.loadClass(TEST_CLASS)
        assert(testClass.declaredMethods.any { it.name == CONTENT_METHOD }) {
            "Test method $CONTENT_METHOD not present on test class $TEST_CLASS"
        }
        if (virtualMachine.allThreads().any { it.isSuspended }) {
            virtualMachine.resume()
        }
        invokeRunnerMainInSeparateProcess(classLoader, proxyPort)

        val manager = virtualMachine.eventRequestManager()

        val loggedItems = mutableListOf<LocatableEvent>()
        var inContentMethod = false
        vmLoop@
        while (true) {
            val eventSet = virtualMachine.eventQueue().remove(1000) ?: continue
            for (event in eventSet) {
                when (event) {
                    is VMDeathEvent, is VMDisconnectEvent -> {
                        break@vmLoop
                    }
                    // We start VM with option 'suspend=n', in case VMStartEvent is still received, discard.
                    is VMStartEvent -> {
                    }
                    is MethodEntryEvent -> {
                        if (!inContentMethod &&
                            event.location().method().name() == CONTENT_METHOD
                        ) {
                            if (manager.stepRequests().isEmpty()) {
                                // Create line stepping request to get all normal line steps starting now.
                                val stepReq = manager.createStepRequest(
                                    event.thread(),
                                    StepRequest.STEP_LINE,
                                    StepRequest.STEP_INTO
                                )
                                stepReq.setSuspendPolicy(SUSPEND_ALL)
                                stepReq.addClassExclusionFilter("java.*")
                                stepReq.addClassExclusionFilter("sun.*")
                                stepReq.addClassExclusionFilter("kotlin.*")
                                stepReq.addClassExclusionFilter("kotlinx.*")
                                stepReq.addClassExclusionFilter("androidx.compose.runtime.*")
                                stepReq.addClassExclusionFilter("jdk.internal.*")

                                // Create class prepare request to be able to set breakpoints on class initializer lines.
                                // There are no line stepping events for class initializers, so we depend on breakpoints.
                                val prepareReq = manager.createClassPrepareRequest()
                                prepareReq.setSuspendPolicy(SUSPEND_ALL)
                                prepareReq.addClassExclusionFilter("java.*")
                                prepareReq.addClassExclusionFilter("sun.*")
                                prepareReq.addClassExclusionFilter("kotlinx.*")
                                prepareReq.addClassExclusionFilter("androidx.compose.runtime.*")
                                prepareReq.addClassExclusionFilter("jdk.internal.*")
                            }
                            manager.stepRequests().map { it.enable() }
                            manager.classPrepareRequests().map { it.enable() }
                            inContentMethod = true
                            loggedItems.add(event)
                        }
                    }
                    is StepEvent -> {
                        // Handle the case where an Exception causing program to exit without MethodExitEvent.
                        if (inContentMethod && event.location().method().name() == "run") {
                            manager.stepRequests().map { it.disable() }
                            manager.classPrepareRequests().map { it.disable() }
                            manager.breakpointRequests().map { it.disable() }
                            break@vmLoop
                        }
                        if (inContentMethod) {
                            loggedItems.add(event)
                        }
                    }
                    is MethodExitEvent -> {
                        if (event.location().method().name() == CONTENT_METHOD) {
                            manager.stepRequests().map { it.disable() }
                            manager.classPrepareRequests().map { it.disable() }
                            manager.breakpointRequests().map { it.disable() }
                            break@vmLoop
                        }
                    }
                    is ClassPrepareEvent -> {
                        if (inContentMethod) {
                            val initializer =
                                event.referenceType().methods().find { it.isStaticInitializer }
                            try {
                                initializer?.allLineLocations()?.forEach {
                                    manager.createBreakpointRequest(it).enable()
                                }
                            } catch (e: AbsentInformationException) {
                                // If there is no line information, do not set breakpoints.
                            }
                        }
                    }
                    is BreakpointEvent -> {
                        if (inContentMethod) {
                            loggedItems.add(event)
                        }
                    }
                    else -> {
                        throw IllegalStateException("event not handled: $event")
                    }
                }
            }
            eventSet.resume()
        }
        virtualMachine.resume()

        return loggedItems
    }
}

private fun ClassLoader?.extractUrls(): List<URL> {
    return (this as? URLClassLoader)?.let {
        it.urLs.toList() + it.parent.extractUrls()
    } ?: emptyList()
}

@Language("kotlin")
private val RUNNER_SOURCES = """
            import androidx.compose.runtime.*
            import kotlinx.coroutines.*
            fun main() {
                val mainScope = CoroutineScope(
                     NonCancellable + Dispatchers.Main
                )
                val recomposer = Recomposer(mainScope.coroutineContext)
                Composition(EmptyApplier(), recomposer).setContent { content() }
            }
            private class EmptyApplier : Applier<Unit> {
                override val current: Unit = Unit
                override fun down(node: Unit) {}
                override fun up() {}
                override fun insertTopDown(index: Int, instance: Unit) {
                    error("Unexpected")
                }
                override fun insertBottomUp(index: Int, instance: Unit) {
                    error("Unexpected")
                }
                override fun remove(index: Int, count: Int) {
                    error("Unexpected")
                }
                override fun move(from: Int, to: Int, count: Int) {
                    error("Unexpected")
                }
                override fun clear() {}
            }
""".trimIndent()
