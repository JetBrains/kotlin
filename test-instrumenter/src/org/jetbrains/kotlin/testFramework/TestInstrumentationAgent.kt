/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.agent.builder.AgentBuilder.InitializationStrategy
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy
import net.bytebuddy.agent.builder.AgentBuilder.TypeStrategy
import net.bytebuddy.asm.Advice
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.matcher.ElementMatchers.named
import net.bytebuddy.matcher.ElementMatchers.none
import org.jetbrains.kotlin.testFramework.inputchecking.InputCheckingFileExistsAdvice
import org.jetbrains.kotlin.testFramework.inputchecking.InputCheckingFileReadAdvice
import org.jetbrains.kotlin.testFramework.inputchecking.UndeclaredInputsGuard
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.nio.file.Paths
import kotlin.io.path.pathString

object TestInstrumentationAgent {
    @JvmStatic
    fun premain(@Suppress("unused") args: String?, instrumentation: Instrumentation) {
        val debug = System.getProperty("test.instrumenter.debug") == "true"
        if (debug) {
            println("org.jetbrains.kotlin.testFramework.TestInstrumentationAgent: premain")
        }

        instrumentMockApplicationCreationTracing(instrumentation, debug)

        if (System.getProperty("test.instrumenter.inputs.check.enabled").toBoolean()) {
            instrumentEmittingCustomJfrEvents(instrumentation)
        }
    }

    private fun instrumentMockApplicationCreationTracing(instrumentation: Instrumentation, debug: Boolean) {
        instrumentation.addTransformer(MockApplicationCreationTracingInstrumenter(debug))
    }

    private fun instrumentEmittingCustomJfrEvents(instrumentation: Instrumentation) {
        initializeUndeclaredInputsGuard()
        installInputCheckingAdvices(instrumentation)
    }

    private fun initializeUndeclaredInputsGuard() {
        val rootDir = System.getProperty("test.instrumenter.root.dir")
        val buildDir = System.getProperty("test.instrumenter.build.dir")
        val failFast = System.getProperty("test.instrumenter.fail.fast").toBoolean()
        val declaredInputs = File(System.getProperty("test.instrumenter.declared.inputs.file"))
            .readLines()
            .filter(String::isNotEmpty)

        val nativeHome = System.getProperty("kotlin.internal.native.test.nativeHome")?.let(Paths::get)
        val nativeTestTarget = System.getProperty("kotlin.internal.native.test.target")
        val klibCacheDir = nativeHome?.resolve("klib/cache")
        val klibStdlibCacheDir = klibCacheDir?.resolve("$nativeTestTarget-gSTATIC-system/stdlib-per-file-cache")

        UndeclaredInputsGuard.initialize(
            rootDir,
            buildDir,
            klibCacheDir?.pathString,
            klibStdlibCacheDir?.pathString,
            declaredInputs,
            failFast
        )
    }

    /**
     * ### InitializationStrategy
     *
     * The [InitializationStrategy.NoOp] is just enough initialization strategy for us, because the bytecode from advices is entirely
     * inlined to instrumented classes, and thus we don't need any auxiliary types to be loaded during initialization.
     *
     * ### RedefinitionStrategy
     *
     * - We use [RedefinitionStrategy.RETRANSFORMATION] to instruct ByteBuddy to use class retransformation.
     * - We don't use [RedefinitionStrategy.REDEFINITION], because RETRANSFORMATION gives us better compatibility with other agents.
     * - We don't use [RedefinitionStrategy.DISABLED], because we wouldn't be able to instrument the already-loaded classes.
     *
     * ### TypeStrategy
     *
     * The [TypeStrategy.Default.DECORATE] instructs ByteBuddy how to produce the bytecode in the [ClassFileTransformer].
     *
     * It's a good fit for us since we only decorate method bodies with advices and don't create any new methods or fields.
     * Such structural class modifications are not allowed by JVM for the already-loaded classes, anyway.
     *
     * The resulting bytecode is the same as if we had used [TypeStrategy.Default.REDEFINE],
     * but the illegal modifications are caught earlier (by ByteBuddy, not JVM).
     */
    private fun installInputCheckingAdvices(instrumentation: Instrumentation) {
        AgentBuilder.Default()
            .ignore(none())
            .with(InitializationStrategy.NoOp.INSTANCE)
            .with(RedefinitionStrategy.RETRANSFORMATION)
            .with(TypeStrategy.Default.DECORATE)
            .type(named("java.io.File"))
            .advice(Advice.to(InputCheckingFileExistsAdvice::class.java).on(named("exists")))
            .type(named("java.io.FileInputStream"))
            .advice(Advice.to(InputCheckingFileReadAdvice::class.java).on(named("read")))
            .type(named("java.io.RandomAccessFile"))
            .advice(Advice.to(InputCheckingFileReadAdvice::class.java).on(named("read")))
            .advice(Advice.to(InputCheckingFileReadAdvice::class.java).on(named("readLine")))
            .type(named("sun.nio.ch.FileChannelImpl"))
            .advice(Advice.to(InputCheckingFileReadAdvice::class.java).on(named("read")))
            .installOn(instrumentation)
    }
}

private fun AgentBuilder.Identified.advice(advice: AsmVisitorWrapper.ForDeclaredMethods) =
    transform { builder, _, _, _, _ -> builder.visit(advice) }
