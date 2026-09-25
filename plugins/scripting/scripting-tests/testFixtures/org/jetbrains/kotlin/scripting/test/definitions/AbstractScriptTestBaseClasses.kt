/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.definitions

import org.jetbrains.kotlin.scripting.test.runners.AbstractFirScriptCodegenTest
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.runners.AbstractFirDiagnosticTestBase

open class AbstractScriptWithCustomDefDiagnosticsTestBase : AbstractFirDiagnosticTestBase(FirParser.LightTree) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureWithCustomScriptDef()
            configureCliLikeScriptPreRefinement()
        }
    }
}

open class AbstractScriptWithCustomDefBlackBoxCodegenTest : AbstractFirScriptCodegenTest(parser = FirParser.LightTree) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureWithCustomScriptDef()
            configureCliLikeScriptPreRefinement()
            useCustomRuntimeClasspathProviders(::ScriptWithCustomDefRuntimeClassPathProvider)
        }
    }
}

fun TestConfigurationBuilder.configureWithCustomScriptDef() {
    useConfigurators(
        ::ScriptWithCustomDefEnvironmentConfigurator
    )
    defaultDirectives {
        +WITH_STDLIB
    }
}

/**
 * Refines the scripts in advance, as the CLI pipeline does. Needed for the compiler-based (LightTree) test hosts only: the Analysis API
 * hosts refine the scripts themselves, via the PSI annotation collecting.
 */
fun TestConfigurationBuilder.configureCliLikeScriptPreRefinement() {
    usePreAnalysisHandlers(::ScriptWithCustomDefPreRefinementHandler)
}
