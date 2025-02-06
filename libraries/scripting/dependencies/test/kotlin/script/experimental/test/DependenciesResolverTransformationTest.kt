/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import junit.framework.TestCase
import kotlin.script.experimental.api.RefineConfigurationOnAnnotationsData
import kotlin.script.experimental.api.RefineScriptCompilationConfigurationHandler
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.refineConfigurationOnAnnotations
import kotlin.script.experimental.dependencies.ConfiguratorWithDependencyResolver
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.withTransformedResolvers

class DependenciesResolverTransformationTest : TestCase() {
    fun testWithTransformedResolvers() {
        val resolverA = FileSystemDependenciesResolver()
        val resolverB = FileSystemDependenciesResolver()

        val configurationA = ScriptCompilationConfiguration {
            val data = RefineConfigurationOnAnnotationsData(emptyList(), DummyConfigurator(resolverA))
            refineConfigurationOnAnnotations.append(data)
        }
        val configurationB = configurationA.withTransformedResolvers { resolverB }

        configurationA.checkHandler {
            assertTrue(it is DummyConfigurator && it.resolver == resolverA)
        }

        configurationB.checkHandler {
            assertTrue(it is DummyConfigurator && it.resolver == resolverB)
        }
    }

    private class DummyConfigurator(
        var resolver: ExternalDependenciesResolver,
    ) : RefineScriptCompilationConfigurationHandler, ConfiguratorWithDependencyResolver<DummyConfigurator> {
        override fun invoke(context: ScriptConfigurationRefinementContext) =
            ScriptCompilationConfiguration(context.compilationConfiguration).asSuccess()

        override fun transformResolver(transform: (ExternalDependenciesResolver) -> ExternalDependenciesResolver) =
            DummyConfigurator(transform(resolver))
    }

    private inline fun ScriptCompilationConfiguration.checkHandler(
        check: (RefineScriptCompilationConfigurationHandler) -> Unit,
    ) {
        val handler = this[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.firstOrNull()?.handler ?: return
        check(handler)
    }
}