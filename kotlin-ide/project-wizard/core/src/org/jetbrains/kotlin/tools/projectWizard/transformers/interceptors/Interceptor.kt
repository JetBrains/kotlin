/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors

import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildFileIR
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import org.jetbrains.kotlin.tools.projectWizard.transformers.Predicate
import org.jetbrains.kotlin.tools.projectWizard.transformers.TransformerFunction

interface Interceptor

typealias InterceptionPointValues = Map<InterceptionPoint<Any>, Any>
typealias SourcesetInterceptionPointValues = Map<Identificator, InterceptionPointValues>

data class TemplateInterceptionApplicationState(
    val buildFileIR: BuildFileIR,
    val moduleToSettings: SourcesetInterceptionPointValues
)

data class TemplateInterceptor(
    val template: Template,
    val applicabilityCheckers: List<Predicate<BuildFileIR>>,
    val buildFileTransformers: List<TransformerFunction<BuildFileIR>>,
    val interceptionPointModifiers: List<InterceptionPointModifier<Any>>
) : Interceptor {
    fun applyTo(state: TemplateInterceptionApplicationState): TemplateInterceptionApplicationState {
        if (applicabilityCheckers.any { checker -> !checker(state.buildFileIR) }) return state

        val modulesWithTemplate = state.buildFileIR.modules.modules.filter { it.template?.id == template.id }
        if (modulesWithTemplate.isEmpty()) return state

        val transformedBuildFile = applyBuildFileTransformers(state.buildFileIR)

        val mutableValues = state.moduleToSettings.toMutableMap()
        for (module in modulesWithTemplate) {
            mutableValues.compute(module.originalModule.identificator) { _, values ->
                applyInterceptionPointModifiers(values.orEmpty())
            }
        }

        return TemplateInterceptionApplicationState(transformedBuildFile, mutableValues)
    }

    private fun applyInterceptionPointModifiers(values: InterceptionPointValues): InterceptionPointValues {
        val mutableValues = values.toMutableMap()
        for (modifier in interceptionPointModifiers) {
            mutableValues.compute(modifier.point) { _, value ->
                modifier.modifier(value ?: modifier.point.initialValue)
            }
        }
        return mutableValues
    }

    private fun applyBuildFileTransformers(buildFile: BuildFileIR): BuildFileIR =
        buildFileTransformers.fold(buildFile) { result, transformer ->
            transformer(result) ?: result
        }
}

fun List<TemplateInterceptor>.applyAll(state: TemplateInterceptionApplicationState) =
    fold(state) { currentState, interceptor ->
        interceptor.applyTo(currentState)
    }

class TemplateInterceptorBuilder<T : Template>(val template: T) {
    private val buildFileTransformers = mutableListOf<TransformerFunction<BuildFileIR>>()
    fun transformBuildFile(transform: TransformerFunction<BuildFileIR>) {
        buildFileTransformers += transform
    }

    private val applicabilityCheckers = mutableListOf<Predicate<BuildFileIR>>()
    fun applicableIf(checker: Predicate<BuildFileIR>) {
        applicabilityCheckers += checker
    }

    private val interceptionPointModifiers = mutableListOf<InterceptionPointModifier<Any>>()
    fun <T : Any> interceptAtPoint(point: InterceptionPoint<T>, modifier: (T) -> T) {
        interceptionPointModifiers.add(InterceptionPointModifier(point, modifier))
    }


    fun build() = TemplateInterceptor(
        template,
        applicabilityCheckers,
        buildFileTransformers,
        interceptionPointModifiers
    )
}


fun <T : Template> interceptTemplate(template: T, builder: TemplateInterceptorBuilder<T>.() -> Unit) =
    TemplateInterceptorBuilder(template).apply(builder).build()