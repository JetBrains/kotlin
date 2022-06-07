/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.samWithReceiver.k2.FirSamWithReceiverExtensionRegistrar
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractFirDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

// ------------------------ diagnostics ------------------------

abstract class AbstractSamWithReceiverTest : AbstractDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

abstract class AbstractFirSamWithReceiverTest : AbstractFirDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

// ------------------------ codegen ------------------------

open class AbstractIrBlackBoxCodegenTestForSamWithReceiver : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

open class AbstractFirBlackBoxCodegenTestForSamWithReceiver : AbstractFirBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::SamWithReceiverEnvironmentConfigurator)
}

class SamWithReceiverEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        private val TEST_ANNOTATIONS = listOf("SamWithReceiver")
    }

    override fun ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        StorageComponentContainerContributor.registerExtension(CliSamWithReceiverComponentContributor(TEST_ANNOTATIONS))
        FirExtensionRegistrarAdapter.registerExtension(FirSamWithReceiverExtensionRegistrar(TEST_ANNOTATIONS))
    }
}
