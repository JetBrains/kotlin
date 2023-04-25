/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.assignment.plugin.k2.FirAssignmentPluginExtensionRegistrar
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.resolve.extensions.AssignResolutionAltererExtension
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

// ------------------------ diagnostics ------------------------

abstract class AbstractAssignmentPluginDiagnosticTest : AbstractDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
        builder.configureDiagnostics()
    }
}

abstract class AbstractFirPsiAssignmentPluginDiagnosticTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
        builder.configureDiagnostics()
        builder.configurationForClassicAndFirTestsAlongside()
    }
}

// ------------------------ codegen ------------------------

open class AbstractIrBlackBoxCodegenTestAssignmentPlugin : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

open class AbstractFirLightTreeBlackBoxCodegenTestForAssignmentPlugin : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

// ------------------------ configuration ------------------------

fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::AssignmentPluginEnvironmentConfigurator)
}

fun TestConfigurationBuilder.configureDiagnostics() {
    defaultDirectives {
        +RENDER_DIAGNOSTICS_FULL_TEXT
    }
}

class AssignmentPluginEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        private val TEST_ANNOTATIONS = listOf(
            "ValueContainer",
            "qualified.ValueContainer",
        )
    }

    @OptIn(InternalNonStableExtensionPoints::class)
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        AssignResolutionAltererExtension.Companion.registerExtension(CliAssignPluginResolutionAltererExtension(TEST_ANNOTATIONS))
        StorageComponentContainerContributor.registerExtension(AssignmentComponentContainerContributor(TEST_ANNOTATIONS))
        FirExtensionRegistrarAdapter.registerExtension(FirAssignmentPluginExtensionRegistrar(TEST_ANNOTATIONS))
    }
}
