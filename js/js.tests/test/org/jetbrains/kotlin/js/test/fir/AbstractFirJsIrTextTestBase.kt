/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.BinaryKind
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.runners.ir.AbstractFirIrTextTestBase
import org.jetbrains.kotlin.test.services.JsLibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

open class AbstractFirJsIrTextTestBase(parser: FirParser) : AbstractFirIrTextTestBase(parser, TargetBackend.JS_IR) {
    override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )

        useAdditionalService(::JsLibraryProvider)
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            globalDefaults {
                targetPlatform = JsPlatforms.defaultJsPlatform
                artifactKind = BinaryKind.NoArtifact
                dependencyKind = DependencyKind.Source
            }
        }
    }
}

open class AbstractFirLightTreeJsIrTextTest : AbstractFirJsIrTextTestBase(FirParser.LightTree)
