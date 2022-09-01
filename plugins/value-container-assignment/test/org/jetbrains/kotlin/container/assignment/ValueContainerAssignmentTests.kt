/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container.assignment

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.assignment.k2.FirValueContainerAssignmentExtensionRegistrar
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.resolve.extensions.AssignResolutionAltererExtension
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractFirDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

// ------------------------ diagnostics ------------------------

abstract class AbstractValueContainerAssignmentTest : AbstractDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

abstract class AbstractFirValueContainerAssignmentTest : AbstractFirDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

// ------------------------ codegen ------------------------

open class AbstractIrBlackBoxCodegenTestForValueContainerAssignment : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

open class AbstractFirBlackBoxCodegenTestForValueContainerAssignment : AbstractFirBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::ValueContainerAssignmentEnvironmentConfigurator)
}

class ValueContainerAssignmentEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        private val TEST_ANNOTATIONS = listOf("ValueContainer")
    }

    @OptIn(InternalNonStableExtensionPoints::class)
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        AssignResolutionAltererExtension.Companion.registerExtension(ValueContainerAssignResolutionAltererExtension(TEST_ANNOTATIONS))
        StorageComponentContainerContributor.registerExtension(ValueContainerAssignmentComponentContainerContributor(TEST_ANNOTATIONS))
        FirExtensionRegistrarAdapter.registerExtension(FirValueContainerAssignmentExtensionRegistrar(TEST_ANNOTATIONS))
    }
}