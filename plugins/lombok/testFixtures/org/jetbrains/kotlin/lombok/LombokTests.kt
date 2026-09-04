/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLDiagnosticsTest
import org.jetbrains.kotlin.lombok.LombokDirectives.ENABLE_LOMBOK
import org.jetbrains.kotlin.test.backend.handlers.IrPrettyKotlinDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.preprocessors.ConfigCommentTransformerPreprocessor
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest

// ---------------------------- box ----------------------------

open class AbstractFirLightTreeBlackBoxCodegenTestForLombok : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()
        builder.configureIrHandlersStep {
            useHandlers(
                ::IrPrettyKotlinDumpHandler
            )
        }
    }
}

// ---------------------------- diagnostics ----------------------------

open class AbstractFirPsiDiagnosticTestForLombok : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()
    }
}

// ---------------------------- LL FIR (IDE) diagnostics ----------------------------

/**
 * Runs the Low Level FIR diagnostics pipeline over Lombok test data.
 *
 * This is the `KtFile.diagnostics(...)` path that the IDE uses for highlighting.
 * It resolves lazily, so it sees a declaration that a Lombok generator left below `BODY_RESOLVE`.
 * The compiler-based [AbstractFirPsiDiagnosticTestForLombok] resolves eagerly and cannot see that.
 *
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLDiagnosticsTest
 */
abstract class AbstractLLLombokDiagnosticsTest : AbstractLLDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()

        // The test data carries a `lombok.config` that only `LombokEnvironmentConfigurator` reads. It is a module
        // file like any other, and the Analysis API builds PSI for every one of those, so it has to be told that
        // this one is a resource rather than a source it failed to recognize.
        builder.defaultDirectives {
            +AnalysisApiTestDirectives.ALLOW_NON_SOURCE_FILES
        }
    }
}

// ---------------------------- configuration ----------------------------

fun TestConfigurationBuilder.enableLombok() {
    defaultDirectives {
        +ENABLE_LOMBOK
    }
    useConfigurators(::LombokEnvironmentConfigurator)
    useCustomRuntimeClasspathProviders(::LombokRuntimeClassPathProvider)
    useSourcePreprocessor(::ConfigCommentTransformerPreprocessor)
}
