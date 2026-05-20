/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.matcher.ElementMatchers.named
import net.bytebuddy.matcher.ElementMatchers.none
import java.lang.instrument.Instrumentation

object TestInstrumentationAgent {
    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        val debug = "debug" in args?.split(",").orEmpty()
        if (debug) {
            println("org.jetbrains.kotlin.testFramework.TestInstrumentationAgent: premain")
        }

        instrumentMockApplicationCreationTracing(instrumentation, debug)

        if (System.getProperty("test.instrumenter.inputs.check.enabled") == "true") {
            instrumentEmittingCustomJfrEvents(instrumentation)
        }
    }

    private fun instrumentMockApplicationCreationTracing(instrumentation: Instrumentation, debug: Boolean) {
        instrumentation.addTransformer(MockApplicationCreationTracingInstrumenter(debug))
    }

    private fun instrumentEmittingCustomJfrEvents(instrumentation: Instrumentation) {
        AgentBuilder.Default()
            .ignore(none())
            // NoOp is just enough initialization strategy, because the bytecode from advices is entirely inlined to instrumented classes,
            // and thus we don't need any auxiliary types to be loaded during initialization.
            .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
            // RETRANSFORMATION instructs ByteBuddy to use Instrumentation.retransformClasses() over Instrumentation.redefineClasses().
            // It gives us better compatibility with other agents, since our transformer can be re-invoked as part of the pipeline.
            // Without retransformClasses() / redefineClasses(), we wouldn't be able to instrument the already-loaded classes.
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            // DECORATE instructs ByteBuddy how to produce the bytecode in the transformer registered via Instrumentation.registerTransformer().
            // It's a good fit for us since we only decorate method bodies with advices, don't create any new methods or fields.
            // Such structural changes are not allowed by JVM for the already-loaded classes, anyway.
            // The resulting bytecode is the same as if we had used REDEFINE, but illegal changes are caught earlier (by ByteBuddy, not JVM).
            .with(AgentBuilder.TypeStrategy.Default.DECORATE)
            .type(named("java.io.File"))
            .advice(Advice.to(FileExistsAdvice::class.java).on(named("exists")))
            .type(named("java.io.FileInputStream"))
            .advice(Advice.to(FileReadAdvice::class.java).on(named("read")))
            .type(named("java.io.RandomAccessFile"))
            .advice(Advice.to(FileReadAdvice::class.java).on(named("read")))
            .advice(Advice.to(FileReadAdvice::class.java).on(named("readLine")))
            .type(named("sun.nio.ch.FileChannelImpl"))
            .advice(Advice.to(FileReadAdvice::class.java).on(named("read")))
            .installOn(instrumentation)
    }
}

private fun AgentBuilder.Identified.advice(advice: AsmVisitorWrapper.ForDeclaredMethods) =
    transform { builder, _, _, _, _ -> builder.visit(advice) }
