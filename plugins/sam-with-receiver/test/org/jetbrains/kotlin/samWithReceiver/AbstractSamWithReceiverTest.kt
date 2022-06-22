/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractSamWithReceiverTest : AbstractDiagnosticTest() {
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

    override fun registerCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        StorageComponentContainerContributor.registerExtension(
            project,
            CliSamWithReceiverComponentContributor(TEST_ANNOTATIONS)
        )
    }
}
