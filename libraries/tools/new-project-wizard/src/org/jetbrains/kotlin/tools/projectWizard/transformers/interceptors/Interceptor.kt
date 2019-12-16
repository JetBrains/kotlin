/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors

import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildFileIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.sourcesets
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import org.jetbrains.kotlin.tools.projectWizard.transformers.Predicate
import org.jetbrains.kotlin.tools.projectWizard.transformers.TransformerFunction

interface Interceptor

data class TemplateInterceptor(
    val applicabilityCheckers: List<Predicate<BuildFileIR>>,
    val buildFileTransformers: List<TransformerFunction<BuildFileIR>>
) : Interceptor {
    fun applyTo(buildFileIR: BuildFileIR): BuildFileIR? {
        if (applicabilityCheckers.any { checker -> !checker(buildFileIR) }) return null

        var isApplied = false
        var result: BuildFileIR = buildFileIR

        for (transformer in buildFileTransformers) {
            val newResult = transformer(result)
            if (newResult != null) {
                result = newResult
                isApplied = true
            }
        }

        return result.takeIf { isApplied }
    }

    fun applyUntilConverge(buildFileIR: BuildFileIR) =
        generateSequence(buildFileIR, { buildFile ->
            applyTo(buildFile)
        }).last()
}

class TemplateInterceptorBuilder<T : Template>(private val template: T) {
    private val buildFileTransformers = mutableListOf<TransformerFunction<BuildFileIR>>()
    fun transformBuildFile(transform: TransformerFunction<BuildFileIR>) {
        buildFileTransformers += transform
    }

    private val applicabilityCheckers = mutableListOf<Predicate<BuildFileIR>>()
    fun applicableIf(checker: Predicate<BuildFileIR>) {
        applicabilityCheckers += checker
    }

    init {
        applicableIf { buildFile ->
            buildFile.sourcesets.any { it.template?.id == template.id }
        }
    }

    fun build() = TemplateInterceptor(applicabilityCheckers, buildFileTransformers)
}

fun List<TemplateInterceptor>.fold() =
    TemplateInterceptor(
        flatMap { it.applicabilityCheckers },
        flatMap { it.buildFileTransformers }
    )


fun <T : Template> interceptTemplate(template: T, builder: TemplateInterceptorBuilder<T>.() -> Unit) =
    TemplateInterceptorBuilder(template).apply(builder).build()