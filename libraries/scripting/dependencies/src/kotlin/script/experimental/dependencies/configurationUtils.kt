/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies

import kotlin.script.experimental.api.RefineConfigurationOnAnnotationsData
import kotlin.script.experimental.api.RefineScriptCompilationConfigurationHandler
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.refineConfigurationOnAnnotations

interface ConfiguratorWithDependencyResolver<C : RefineScriptCompilationConfigurationHandler> {
    fun transformResolver(transform: (ExternalDependenciesResolver) -> ExternalDependenciesResolver): C
}

fun ScriptCompilationConfiguration.withTransformedResolvers(
    transform: (ExternalDependenciesResolver) -> ExternalDependenciesResolver,
) = ScriptCompilationConfiguration(this) {
    refineConfigurationOnAnnotations.transform {
        val handler = it.handler as? ConfiguratorWithDependencyResolver<*> ?: return@transform it
        RefineConfigurationOnAnnotationsData(it.annotations, handler.transformResolver(transform))
    }
}
