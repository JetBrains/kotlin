/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.irText

import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.runners.ir.AbstractNonJvmIrTextTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator

abstract class AbstractNativeIrTextTestBase<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>> :
    AbstractNonJvmIrTextTest<FrontendOutput>(NativePlatforms.unspecifiedNativePlatform, TargetBackend.NATIVE) {

    final override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::NativeEnvironmentConfigurator,
        )

        useAdditionalService(::LibraryProvider)
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                // Kotlin/Native does not have "minimal" stdlib(like other backends do), so full stdlib is needed to resolve
                // `Any`, `String`, `println`, etc.
                +ConfigurationDirectives.WITH_STDLIB
            }
        }
    }
}
