/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

open class AbstractSwiftExportContextTest : AbstractSwiftExportContextTestBase(TargetBackend.JVM) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.apply {
            globalDefaults {
                targetBackend = TargetBackend.JVM
            }
        }
    }
}

abstract class AbstractSwiftExportContextTestBase(
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
        )

        facadeStep(::JvmCompilerWithSwiftExportPluginFacade)

        handlersStep(SwiftExportArtifact.Kind) {
            useHandlers(::SirExportHandler)
        }
    }
}

