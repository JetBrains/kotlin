/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts.uklibsPublication

import org.jetbrains.kotlin.Uklib
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.transformKGPModelToUklibModel
import java.io.File

internal fun uklibFromKGPModel(
    targets: List<KotlinTarget>,
    onPublishCompilation: (KotlinCompilation<*>) -> Unit = {}
): Uklib<String> {
    val compilationToArtifact = mutableMapOf<KotlinCompilation<*>, Iterable<File>>()

    targets.forEach { target ->
        when (target) {
            is KotlinJsIrTarget -> {
                error("...")
//                    val mainComp = target.compilations.getByName(MAIN_COMPILATION_NAME)
//                    compilationToArtifact[mainComp] = mainComp.out
            }
            is KotlinJvmTarget -> {
                val mainComp = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                // FIXME: How do we handle that there are multiple classesDir?
                /**
                 * What should we do with multiple classes output:
                 *
                 * 1. What is going to happen if Foo collides across Kotlin and Java compilations. Is one picked over the other or is there a
                 * class redeclaration error?
                 * 2. Publication always has a single jar
                 *
                 * TODO: Discuss this case in text
                 */
                compilationToArtifact[mainComp] = mainComp.output.classesDirs
                onPublishCompilation(mainComp)
            }
            is KotlinNativeTarget -> {
                val mainComp = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                compilationToArtifact[mainComp] = listOf(
                    // FIXME: Make this lazy
                    // FIXME: We have to unzip this
                    // FIXME: 13.11 - Just use unpacked compilation directly
                    mainComp.compileTaskProvider.flatMap {
                        it.outputFile
                    }.get()
                )
                onPublishCompilation(mainComp)
            }
            // FIXME: Metadata target forms a natural bamboo with default target hierarchy
            /**
             * What to do with bamboo and default target hierarchy: this is actually a problem because this works in KGP, but with Uklibs
             * this is not going to work
             *
             *
             * 1. Can we refactor metadata compilations to compile all same-target compilations: no because expect/actuals must not be in the same compilation
             * But maybe we could relax this inside the compiler
             *
             * 2. Source set target context feature? It will allow declaring e.g. commonMain that is constrained to ios and jvm, but providing jvm
             * will not be required.
             *
             * 3. Prohibit bamboo source set configuration in. This will work for cases where there are no expect/actuals across metadata compilations
             *
             * Typical use case:
             *
             * TODO: Write communication about this in the channel
             */
            is KotlinMetadataTarget -> {
                target.compilations
                    // Probably this is not needed
                    .filterNot { it is KotlinCommonCompilation && !it.isKlibCompilation }
                    .forEach { compilation ->
                        // FIXME: Aren't test compilations going to be here?
                        compilationToArtifact[compilation] = compilation.output.classesDirs
                        onPublishCompilation(compilation)
                    }
            }
        }
    }

    return transformKGPModelToUklibModel(
        "stub",
        publishedCompilations = compilationToArtifact.keys.toList(),
        publishedArtifact = { compilationToArtifact[this]!!.single() },
        defaultSourceSet = { this.defaultSourceSet },
        target = { this.target.targetName },
        dependsOn = { this.dependsOn },
        identifier = { this.name }
    )
}