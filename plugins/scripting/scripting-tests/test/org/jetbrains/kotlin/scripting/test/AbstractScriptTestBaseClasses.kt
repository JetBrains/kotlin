/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.runners.AbstractFirDiagnosticTestBase
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirScriptCodegenTest

open class AbstractScriptWithCustomDefDiagnosticsTestBase : AbstractFirDiagnosticTestBase(FirParser.Psi) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureWithCustomScriptDef()
        }
    }
}

open class AbstractScriptWithCustomDefBlackBoxCodegenTest : AbstractFirScriptCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureWithCustomScriptDef()
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