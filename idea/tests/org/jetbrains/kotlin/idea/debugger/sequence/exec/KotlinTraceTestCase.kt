/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.sequence.exec

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.OutputChecker
import com.intellij.debugger.streams.lib.LibrarySupportProvider
import com.intellij.debugger.streams.psi.DebuggerPositionResolver
import com.intellij.debugger.streams.psi.impl.DebuggerPositionResolverImpl
import com.intellij.debugger.streams.trace.*
import com.intellij.debugger.streams.trace.impl.TraceResultInterpreterImpl
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.xdebugger.XDebugSessionListener
import com.sun.jdi.Value
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerTestBase
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

abstract class KotlinTraceTestCase : KotlinDebuggerTestBase() {
    private companion object {
        val DEFAULT_CHAIN_SELECTOR = ChainSelector.byIndex(0)
    }

    private lateinit var traceChecker: StreamTraceChecker

    override fun initOutputChecker(): OutputChecker {
        traceChecker = StreamTraceChecker(this)
        return super.initOutputChecker()
    }

    abstract val librarySupportProvider: LibrarySupportProvider

    fun doTest(filePath: String) = doTestImpl(filePath)

    override fun createDebugProcess(path: String) {
        val filePath = Paths.get(path)
        FileBasedIndex.getInstance().requestReindex(VfsUtil.findFileByIoFile(filePath.toFile(), true)!!)
        val packagePath = StringUtil.substringAfterLast(filePath.parent.toAbsolutePath().toString(), "src${File.separatorChar}")
                ?: throw AssertionError("test data must be placed into test app project in 'src' directory")

        val fileName = filePath.getName(filePath.nameCount - 1).toString()
        val packageName = packagePath.replace(File.separatorChar, '.')
        createLocalProcess("$packageName.${fileName.replace(".kt", "Kt")}")
    }

    @Throws(ExecutionException::class)
    private fun doTestImpl(path: String, chainSelector: ChainSelector = DEFAULT_CHAIN_SELECTOR) {
        createDebugProcess(path)
        val session = debuggerSession.xDebugSession ?: kotlin.test.fail("XDebugSession is null")
        TestCase.assertNotNull(session)

        val completed = AtomicBoolean(false)
        val positionResolver = getPositionResolver()
        val chainBuilder = getChainBuilder()
        val resultInterpreter = getResultInterpreter()
        val expressionBuilder = getExpressionBuilder()

        session.addSessionListener(object : XDebugSessionListener {
            override fun sessionPaused() {
                if (completed.getAndSet(true)) {
                    resume()
                    return
                }
                try {
                    sessionPausedImpl()
                } catch (t: Throwable) {
                    println("Exception caught: " + t + ", " + t.message, ProcessOutputTypes.SYSTEM)
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

            private fun complete(
                chain: StreamChain?,
                result: TracingResult?,
                error: String?,
                errorReason: FailureReason?
            ) {
                try {
                    if (error != null) {
                        TestCase.assertNotNull(errorReason)
                        TestCase.assertNotNull(chain)
                        handleError(chain!!, error, errorReason!!)
                    } else {
                        TestCase.assertNull(errorReason)
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

    protected fun handleError(chain: StreamChain, error: String, reason: FailureReason) {
        TestCase.fail(error)
    }

    protected fun handleSuccess(chain: StreamChain?, result: TracingResult?) {
        TestCase.assertNotNull(chain)
        TestCase.assertNotNull(result)

        println(chain!!.text, ProcessOutputTypes.SYSTEM)

        val resultValue = result!!.result
        handleResultValue(resultValue.value)

        val trace = result.trace
        traceChecker.checkChain(trace)

        val resolvedTrace = result.resolve(librarySupportProvider.librarySupport.resolverFactory)
        traceChecker.checkResolvedChain(resolvedTrace)
    }

    private fun handleResultValue(result: Value?) {
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