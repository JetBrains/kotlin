/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders

import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.typeProviders.generatedCode.*
import kotlin.script.experimental.util.filterByAnnotationType

internal class AnnotationBasedTypeProvidersRefineScriptCompilationConfigurationHandler(
    private val wrappers: Iterable<AnnotationBasedTypeProviderWrapper>
) : RefineScriptCompilationConfigurationHandler {

    override fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
            ?: return context.compilationConfiguration.asSuccess()

        val typeProviderContext = AnnotationBasedTypeProvider.Context(
            script = context.script,
            compilationConfiguration = context.compilationConfiguration
        )

        val generatedCode = wrappers
            .mapSuccess { it(annotations, typeProviderContext) }
            .onSuccess { GeneratedCode(it).asSuccess() }
            .valueOr { return it }

        // TODO: Maybe we should check for name collisions?
        return context.compilationConfiguration.provide(generatedCode).asSuccess()
    }

}

internal interface AnnotationBasedTypeProviderWrapper {
    val annotationType: KClass<out Annotation>
    operator fun invoke(annotations: List<ScriptSourceAnnotation<*>>, context: AnnotationBasedTypeProvider.Context): ResultWithDiagnostics<GeneratedCode>
}

internal fun <A : Annotation> AnnotationBasedTypeProvider<A>.wrapper(
    annotationType: KClass<A>
): AnnotationBasedTypeProviderWrapper = object : AnnotationBasedTypeProviderWrapper {
    override val annotationType = annotationType

    override fun invoke(annotations: List<ScriptSourceAnnotation<*>>, context: AnnotationBasedTypeProvider.Context): ResultWithDiagnostics<GeneratedCode> {
        val castedAnnotations = annotations
            .filterByAnnotationType(annotationType)
            .takeIf { it.isNotEmpty() } ?: return GeneratedCode.Empty.asSuccess()

        return this@wrapper(castedAnnotations, context)
    }
}