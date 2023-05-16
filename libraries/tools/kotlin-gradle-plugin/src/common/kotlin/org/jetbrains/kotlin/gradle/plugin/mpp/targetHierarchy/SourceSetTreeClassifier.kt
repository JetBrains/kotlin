/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.SourceSetTree
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilationFactory
import org.jetbrains.kotlin.tooling.core.extrasLazyProperty

/**
 * Classifier providing the corresponding [SourceSetTree] associated with any given [KotlinCompilation]
 *
 * ### Example: Overwriting 'test' compilations [SourceSetTreeClassifier]:
 * Consider the following setup:
 * ```kotlin
 * kotlin {
 *     val testCompilation = jvm().compilations.getByName("test")
 * }
 * ```
 *
 * In this example, we know that the 'jvm/test' compilation will have a 'jvmTest' SourceSet which
 * will depend on 'commonTest' and therefore is part of the 'test' [SourceSetTree].
 *
 * When another [SourceSetTreeClassifier] is specified, this behaviour is changed.
 * Using the External Kotlin Target API:
 * ```kotlin
 *   myTarget.createCompilation {
 *         compilationName = "test"
 *         sourceSetTreeClassifier = SourceSetTreeClassifier.Name("unitTest")
 *   }
 * ```
 *
 * This will create a compilation called 'test' which however will be considered part of the 'unitTest' SourceSetTree.
 * The SourceSet of this 'jvm/test' compilation will still be called 'jvmTest' but since its part of the 'unitTest [SourceSetTree],
 * there will not be a dependsOn edge to 'commonTest', but (if present) 'commonUnitTest'
 */
@ExternalKotlinTargetApi
sealed class SourceSetTreeClassifier {

    /**
     * Default Classifier: The name of the compilation will be used to infer the [SourceSetTree]:
     * 'main' compilations will be part of [SourceSetTree.main]
     * 'test' compilations will be part of [SourceSetTree.test]
     * ...
     */
    @ExternalKotlinTargetApi
    object Default : SourceSetTreeClassifier() {
        override fun toString(): String = "Default"
    }

    /**
     * Indicates that the given compilations is not part of any 'named' [SourceSetTree].
     * Neither [KotlinTargetHierarchy] will be applied nor default dependsOn edges shall be set.
     */
    @ExternalKotlinTargetApi
    object None : SourceSetTreeClassifier() {
        override fun toString(): String = "None"
    }

    /**
     * Predefined [SourceSetTree] using the [tree] specified.
     */
    @ExternalKotlinTargetApi
    data class Value(val tree: SourceSetTree) : SourceSetTreeClassifier()

    /**
     * Predefined [SourceSetTree] using the [name] specified
     */
    @ExternalKotlinTargetApi
    data class Name(val name: String) : SourceSetTreeClassifier()

    /**
     * Wrapper around [org.gradle.api.provider.Property] of a given [SourceSetTree] in order to
     * make the [SourceSetTree] configurable.
     */
    @ExternalKotlinTargetApi
    class Property(val property: org.gradle.api.provider.Property<SourceSetTree>) : SourceSetTreeClassifier() {
        override fun toString(): String {
            return property.toString()
        }
    }

    internal suspend fun classify(compilation: KotlinCompilation<*>): SourceSetTree? {
        return when (this) {
            is Default -> SourceSetTree(compilation.name)
            is Property -> property.awaitFinalValue()
            is Value -> tree
            is Name -> SourceSetTree(name)
            is None -> null
        }
    }
}

/**
 * Returns the classifier configured for a given compilation.
 * This is writable, as Android requires overwriting of this default behaviour.
 *
 * - The KGP maintained target will set the classifier within the [KotlinJvmAndroidCompilationFactory]
 * - The external Android target will set this classifier within the 'createCompilation'
 *
 * It is therefore safe to access this value as soon as a compilation is provided
 */
internal var KotlinCompilation<*>.sourceSetTreeClassifier: SourceSetTreeClassifier by extrasLazyProperty {
    SourceSetTreeClassifier.Default
}


/**
 * Returns the [SourceSetTree] of a given [KotlinCompilation]:
 * Uses the [sourceSetTreeClassifier] under the hood.
 * See [SourceSetTreeClassifier]
 */
internal suspend fun SourceSetTree.Companion.orNull(compilation: KotlinCompilation<*>) =
    compilation.sourceSetTreeClassifier.classify(compilation)
