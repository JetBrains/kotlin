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
import androidx.compose.compiler.plugins.kotlin.CodegenTestFiles
import androidx.compose.compiler.plugins.kotlin.debug.clientserver.TestProxy
import androidx.compose.compiler.plugins.kotlin.tmpDir
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
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.psi.KtFile
import java.net.URL
import java.net.URLClassLoader

private const val RUNNER_CLASS = "RunnerKt"
private const val MAIN_METHOD = "main"
private const val CONTENT_METHOD = "content"
private const val TEST_CLASS = "TestKt"

abstract class AbstractDebuggerTest : AbstractCodegenTest() {
    private lateinit var virtualMachine: VirtualMachine
    private var proxyPort: Int = -1
    private lateinit var methodEntryRequest: MethodEntryRequest
    private lateinit var methodExitRequest: MethodExitRequest

    fun initialize(vm: VirtualMachine, port: Int) {
        virtualMachine = vm
        proxyPort = port
    }

    override fun setUp() {
        super.setUp()
        if (proxyPort == -1) throw error("initialize method must be called on AbstractDebuggerTest")
        createMethodEventsForTestClass()
    }

    override fun tearDown() {
        super.tearDown()
        virtualMachine.eventRequestManager()
            .deleteEventRequests(listOf(methodEntryRequest, methodExitRequest))
    }

    private fun createMethodEventsForTestClass() {
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

    private fun invokeRunnerMainInSeparateProcess(
        classLoader: URLClassLoader,
        port: Int
    ) {
        val classPath = classLoader.extractUrls().toMutableList()
        if (classLoader is GeneratedClassLoader) {
            val outDir = tmpDir("${this::class.simpleName}_${this.name}")
            val currentOutput = SimpleOutputFileCollection(classLoader.allGeneratedFiles)
            currentOutput.writeAllTo(outDir)
            classPath.add(0, outDir.toURI().toURL())
        }
        TestProxy(port, RUNNER_CLASS, MAIN_METHOD, classPath).runTest()
    }

    fun collectDebugEvents(@Language("kotlin") source: String): List<LocatableEvent> {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        files.add(sourceFile("Runner.kt", RUNNER_SOURCES))
        files.add(sourceFile("Test.kt", source))
        myFiles = CodegenTestFiles.create(files)
        return doTest()
    }

    private fun doTest(): List<LocatableEvent> {
        val classLoader = createClassLoader()
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