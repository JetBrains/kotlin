/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.runners

import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest

abstract class AbstractFirJsPlainObjectsPluginDiagnosticTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            globalDefaults {
                targetPlatform = JsPlatforms.defaultJsPlatform
                targetBackend = TargetBackend.JS_IR
                dependencyKind = DependencyKind.Source
            }

            configureForKotlinxJsPlainObjects()
            disableOptInErrors()
        }
    }
}

private fun TestConfigurationBuilder.disableOptInErrors() {
    defaultDirectives {
        DIAGNOSTICS with listOf("-OPT_IN_USAGE", "-OPT_IN_USAGE_ERROR")
    }
}
