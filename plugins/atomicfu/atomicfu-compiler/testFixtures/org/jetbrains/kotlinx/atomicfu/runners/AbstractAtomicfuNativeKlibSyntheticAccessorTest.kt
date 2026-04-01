/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.runners

import org.jetbrains.kotlin.config.PartialLinkageLogLevel
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createSimpleTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CustomKlibs
import org.jetbrains.kotlin.konan.test.syntheticAccessors.AbstractNativeKlibSyntheticAccessorTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

abstract class AbstractAtomicfuNativeKlibSyntheticAccessorTest : AbstractNativeKlibSyntheticAccessorTest(
    /*
     * As we skip C-interop KLIBs in the classpath, we don't need to fail the test if PL detects any incompatibilities.
     *
     * TODO (KT-85312): Switch to the default log level (ERROR).
     */
    partialLinkageLogLevel = PartialLinkageLogLevel.SILENT
) {
    private lateinit var extensionContext: ExtensionContext

    @RegisterExtension
    val extensionContextCaptor = BeforeEachCallback { context ->
        this.extensionContext = context
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)

        with(builder) {
            useConfigurators(::AtomicfuEnvironmentConfigurator)
            useCustomRuntimeClasspathProviders(
                ::AtomicfuNativeRuntimeClasspathProvider.bind(
                    extensionContext.createSimpleTestRunSettings().get<CustomKlibs>()
                )
            )
        }
    }
}

private class AtomicfuNativeRuntimeClasspathProvider(
    testServices: TestServices,
    private val customKlibs: CustomKlibs,
) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        /*
         * Exclude C-interop KLIBs from the classpath:
         * - They are not needed for the test, as the test executes only the first stage of compilation and
         *   such KLIBs represent not the AtomicFU API but internal implementation details.
         * - We don't support deserialization of IR from C-interop KLIBs in tests yet.
         *
         * TODO (KT-85312): Lift the "-cinterop" filter.
         */
        return customKlibs.klibs.filterNot { "-cinterop" in it.name }
    }
}
