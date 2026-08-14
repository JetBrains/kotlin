/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.services.ComposeJsClasspathProvider
import androidx.compose.compiler.plugins.kotlin.services.ComposeJvmClasspathConfigurator
import org.jetbrains.kotlin.js.test.runners.AbstractLightTreeJsIrTextTest
import org.jetbrains.kotlin.test.Constructor2
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.DUMP_KLIB_ABI
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.runners.ir.AbstractJvmIrTextTest
import org.jetbrains.kotlin.test.services.TestServices

object ComposeIrTextDirectives : SimpleDirectivesContainer() {
    val DUMP_ALL_IR by directive("Dump the IR produced by the Compose plugin into a per-target file")
}

/**
 * Dumps the IR the Compose plugin produced, for one target, into `<test name>.<target>.ir.txt`.
 *
 * Unlike the golden tests in `integration-tests`, which print a Kotlin-like rendering, these dump
 * the IR tree itself, so declared types are visible verbatim: nullability, type arguments, and which
 * declaration a type parameter belongs to.
 *
 * The stock IR text handlers write `<test name>.ir.txt`, a name that says nothing about the target,
 * so several target suites sharing one testData directory would overwrite each other. Upstream works
 * around that by keeping one target's dump whole and storing every other target as a
 * `*.<backend>.patch` diff against it. Patches are compact but hard to read, and reading these dumps
 * side by side is the entire point here, so every target gets a whole dump of its own instead.
 */
private fun TestConfigurationBuilder.dumpComposeIrPerTarget(target: String) {
    useDirectives(ComposeIrTextDirectives)
    defaultDirectives {
        // Off: these write target-agnostic file names, so they would collide between the suites.
        -DUMP_IR
        -DUMP_KT_IR
        -DUMP_KLIB_ABI
        +ComposeIrTextDirectives.DUMP_ALL_IR
    }
    configureIrHandlersStep {
        val handler: Constructor2<BackendKind<IrBackendInput>, IrTextDumpHandler> =
            { testServices: TestServices, artifactKind: BackendKind<IrBackendInput> ->
                IrTextDumpHandler(
                    testServices,
                    artifactKind,
                    customExtension = "$target.ir.txt",
                    directive = ComposeIrTextDirectives.DUMP_ALL_IR,
                )
            }
        useHandlers(handler)
    }
}

open class AbstractJvmIrTextTestForCompose : AbstractJvmIrTextTest(FirParser.LightTree) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.composeCompilerPluginConfiguration()
        builder.useConfigurators(::ComposeJvmClasspathConfigurator)
        builder.dumpComposeIrPerTarget("jvm")
    }
}

open class AbstractJsIrTextTestForCompose : AbstractLightTreeJsIrTextTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.composeCompilerPluginConfiguration()
        builder.useCustomRuntimeClasspathProviders(::ComposeJsClasspathProvider)
        builder.dumpComposeIrPerTarget("js")
    }
}
