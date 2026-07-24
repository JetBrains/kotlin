/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.runners

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createSimpleTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CustomKlibs
import org.jetbrains.kotlin.konan.test.diagnostics.AbstractNativeDiagnosticsWithBackendWithInlinedFunInKlibTestBase
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

open class AbstractAtomicfuNativeWithInlinedFunInKlibDiagnosticTest : AbstractNativeDiagnosticsWithBackendWithInlinedFunInKlibTestBase() {
    private lateinit var extensionContext: ExtensionContext

    @RegisterExtension
    val extensionContextCaptor = BeforeEachCallback { context ->
        this.extensionContext = context
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
            useConfigurators(::AtomicfuEnvironmentConfigurator)
            useCustomRuntimeClasspathProviders(
                ::AtomicfuNativeRuntimeClasspathProvider.bind(
                    extensionContext.createSimpleTestRunSettings().get<CustomKlibs>()
                )
            )
        }
    }
}
