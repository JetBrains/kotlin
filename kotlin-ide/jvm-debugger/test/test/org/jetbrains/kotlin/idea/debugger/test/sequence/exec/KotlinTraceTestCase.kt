/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.sequence.exec

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.OutputChecker
import com.intellij.debugger.streams.lib.LibrarySupportProvider
import com.intellij.debugger.streams.psi.DebuggerPositionResolver
import com.intellij.debugger.streams.psi.impl.DebuggerPositionResolverImpl
import com.intellij.debugger.streams.trace.*
import com.intellij.debugger.streams.trace.impl.TraceResultInterpreterImpl
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.xdebugger.XDebugSessionListener
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.debugger.test.KotlinDescriptorTestCaseWithStepping
import org.jetbrains.kotlin.idea.debugger.test.TestFiles
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferences
import java.util.concurrent.atomic.AtomicBoolean

abstract class KotlinTraceTestCase : KotlinDescriptorTestCaseWithStepping() {
    private companion object {
        val DEFAULT_CHAIN_SELECTOR = ChainSelector.byIndex(0)
    }

    private lateinit var traceChecker: StreamTraceChecker

    override fun initOutputChecker(): OutputChecker {
        traceChecker = StreamTraceChecker(this)
        return super.initOutputChecker()
    }

    abstract val librarySupportProvider: LibrarySupportProvider

    override fun doMultiFileTest(files: TestFiles, preferences: DebuggerPreferences) {
        // Sequence expressions are verbose. Disable expression logging for sequence debugger
        KotlinDebuggerCaches.LOG_COMPILATIONS = false

        val session = debuggerSession.xDebugSession ?: kotlin.test.fail("XDebugSession is null")
        assertNotNull(session)

        val completed = AtomicBoolean(false)
        val positionResolver = getPositionResolver()
        val chainBuilder = getChainBuilder()
        val resultInterpreter = getResultInterpreter()
        val expressionBuilder = getExpressionBuilder()

        val chainSelector = DEFAULT_CHAIN_SELECTOR

        session.addSessionListener(object : XDebugSessionListener {
            override fun sessionPaused() {
                if (completed.getAndSet(true)) {
                    resume()
                    return
                }
                try {
                    sessionPausedImpl()
                } catch (t: Throwable) {
                    println("Exception caught: $t, ${t.message}", ProcessOutputTypes.SYSTEM)
                    t.printStackTrace()

                    resume()
                }

            }

            private fun sessionPausedImpl() {
                printContext(debugProcess.debuggerContext)
                val chain = ApplicationManager.getApplication().runReadAction(
                    Computable<StreamChain> {
                        val elementAtBreakpoint = positionResolver.getNearestElementToBreakpoint(session)
                        val chains = if (elementAtBreakpoint == null) null else chainBuilder.build(elementAtBreakpoint)
                        if (chains == null || chains.isEmpty()) null else chainSelector.select(chains)
                    })

                if (chain == null) {
                    complete(null, null, null, FailureReason.CHAIN_CONSTRUCTION)
                    return
                }

                EvaluateExpressionTracer(session, expressionBuilder, resultInterpreter).trace(chain, object : TracingCallback {
                    override fun evaluated(result: TracingResult, context: EvaluationContextImpl) {
                        complete(chain, result, null, null)
                    }

                    override fun evaluationFailed(traceExpression: String, message: String) {
                        complete(chain, null, message, FailureReason.EVALUATION)
                    }

                    override fun compilationFailed(traceExpression: String, message: String) {
                        complete(chain, null, message, FailureReason.COMPILATION)
                    }
                })
            }

            private fun complete(chain: StreamChain?, result: TracingResult?, error: String?, errorReason: FailureReason?) {
                try {
                    if (error != null) {
                        assertNotNull(errorReason)
                        assertNotNull(chain)
                        throw AssertionError(error)
                    } else {
                        assertNull(errorReason)
                        handleSuccess(chain, result)
                    }
                } catch (t: Throwable) {
                    println("Exception caught: " + t + ", " + t.message, ProcessOutputTypes.SYSTEM)
                } finally {
                    resume()
                }
            }

            private fun resume() {
                ApplicationManager.getApplication().invokeLater { session.resume() }
            }
        }, testRootDisposable)
    }

    private fun getPositionResolver(): DebuggerPositionResolver {
        return DebuggerPositionResolverImpl()
    }

    protected fun handleSuccess(chain: StreamChain?, result: TracingResult?) {
        kotlin.test.assertNotNull(chain)
        kotlin.test.assertNotNull(result)

        println(chain.text, ProcessOutputTypes.SYSTEM)

        val trace = result.trace
        traceChecker.checkChain(trace)

        val resolvedTrace = result.resolve(librarySupportProvider.librarySupport.resolverFactory)
        traceChecker.checkResolvedChain(resolvedTrace)
    }

    private fun getResultInterpreter(): TraceResultInterpreter {
        return TraceResultInterpreterImpl(librarySupportProvider.librarySupport.interpreterFactory)
    }

    private fun getChainBuilder(): StreamChainBuilder {
        return librarySupportProvider.chainBuilder
    }

    private fun getExpressionBuilder(): TraceExpressionBuilder {
        return librarySupportProvider.getExpressionBuilder(project)
    }

    protected enum class FailureReason {
        COMPILATION, EVALUATION, CHAIN_CONSTRUCTION
    }

    @FunctionalInterface
    protected interface ChainSelector {
        fun select(chains: List<StreamChain>): StreamChain

        companion object {
            fun byIndex(index: Int): ChainSelector {
                return object : ChainSelector {
                    override fun select(chains: List<StreamChain>): StreamChain = chains[index]
                }
            }
        }
    }
}