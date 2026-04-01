/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticLightTreeTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBytecodeListingTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

// ---------------------------- codegen ----------------------------

open class AbstractFirLightTreeBlackBoxCodegenTestForNoArg : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableNoArg()
    }
}

// ---------------------------- bytecode ----------------------------

open class AbstractFirLightTreeBytecodeListingTestForNoArg : AbstractFirLightTreeBytecodeListingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableNoArg()
    }
}

// ---------------------------- diagnostic ----------------------------

abstract class AbstractFirPsiDiagnosticsTestForNoArg : AbstractPhasedJvmDiagnosticLightTreeTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableNoArg()
    }
}

// ---------------------------- configurator ----------------------------

private fun TestConfigurationBuilder.enableNoArg() {
    useConfigurators(::NoArgEnvironmentConfigurator)
}

class NoArgEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        private val NOARG_ANNOTATIONS = listOf("NoArg", "NoArg2", "test.NoArg")
    }

    override val directiveContainers: List<DirectivesContainer> = listOf(NoArgDirectives)

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        NoArgComponentRegistrar.registerNoArgComponents(this, configuration, NOARG_ANNOTATIONS, NoArgDirectives.INVOKE_INITIALIZERS in module.directives)
    }
}

object NoArgDirectives : SimpleDirectivesContainer() {
    val INVOKE_INITIALIZERS by directive("Enable 'Invoke initializers' mode")
}
