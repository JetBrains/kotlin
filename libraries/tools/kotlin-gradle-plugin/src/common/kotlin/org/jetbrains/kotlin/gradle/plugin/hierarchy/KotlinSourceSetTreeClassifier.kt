/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.hierarchy

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilationFactory
import org.jetbrains.kotlin.tooling.core.extrasLazyProperty

/**
 * Classifier providing the corresponding [KotlinSourceSetTree] associated with any given [KotlinCompilation]
 *
 * ### Example: Overwriting 'test' compilations [KotlinSourceSetTreeClassifier]:
 * Consider the following setup:
 * ```kotlin
 * kotlin {
 *     val testCompilation = jvm().compilations.getByName("test")
 * }
 * ```
 *
 * In this example, we know that the 'jvm/test' compilation will have a 'jvmTest' SourceSet which
 * will depend on 'commonTest' and therefore is part of the 'test' [KotlinSourceSetTree].
 *
 * When another [KotlinSourceSetTreeClassifier] is specified, this behaviour is changed.
 * Using the External Kotlin Target API:
 * ```kotlin
 *   myTarget.createCompilation {
 *         compilationName = "test"
 *         sourceSetTreeClassifier = SourceSetTreeClassifier.Name("unitTest")
 *   }
 * ```
 *
 * This will create a compilation called 'test' which however will be considered part of the 'unitTest' SourceSetTree.
 * The SourceSet of this 'jvm/test' compilation will still be called 'jvmTest' but since its part of the 'unitTest [KotlinSourceSetTree],
 * there will not be a dependsOn edge to 'commonTest', but (if present) 'commonUnitTest'
 */
@ExternalKotlinTargetApi
sealed interface KotlinSourceSetTreeClassifier {

    /**
     * Default Classifier: The name of the compilation will be used to infer the [KotlinSourceSetTree]:
     * 'main' compilations will be part of [KotlinSourceSetTree.main]
     * 'test' compilations will be part of [KotlinSourceSetTree.test]
     * ...
     */
    @ExternalKotlinTargetApi
    object Default : KotlinSourceSetTreeClassifier {
        override fun toString(): String = "Default"
    }

    /**
     * Indicates that the given compilations is not part of any 'named' [KotlinSourceSetTree].
     * No [KotlinHierarchyTemplate] will be applied nor default dependsOn edges shall be set.
     */
    @ExternalKotlinTargetApi
    object None : KotlinSourceSetTreeClassifier {
        override fun toString(): String = "None"
    }

    /**
     * Predefined [KotlinSourceSetTree] using the [tree] specified.
     */
    @ExternalKotlinTargetApi
    data class Value(val tree: KotlinSourceSetTree) : KotlinSourceSetTreeClassifier

    /**
     * Predefined [KotlinSourceSetTree] using the [name] specified
     */
    @ExternalKotlinTargetApi
    data class Name(val name: String) : KotlinSourceSetTreeClassifier

    /**
     * Wrapper around [org.gradle.api.provider.Property] of a given [KotlinSourceSetTree] in order to
     * make the [KotlinSourceSetTree] configurable.
     */
    @ExternalKotlinTargetApi
    class Property(val property: org.gradle.api.provider.Property<KotlinSourceSetTree>) : KotlinSourceSetTreeClassifier {
        override fun toString(): String {
            return property.toString()
        }
    }
}

/**
 * To support old API (moved from .mpp.targetHierarchy to .hierarchy package)
 */
@Suppress("DEPRECATION")
internal class SourceSetTreeClassifierWrapper(
    val classifier: org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.SourceSetTreeClassifier,
) : KotlinSourceSetTreeClassifier {
    override fun toString(): String {
        return classifier.toString()
    }
}

internal suspend fun KotlinSourceSetTreeClassifier.classify(compilation: KotlinCompilation<*>): KotlinSourceSetTree? {
    return when (this) {
        is KotlinSourceSetTreeClassifier.Default -> KotlinSourceSetTree(compilation.name)
        is KotlinSourceSetTreeClassifier.Property -> property.awaitFinalValue()
        is KotlinSourceSetTreeClassifier.Value -> tree
        is KotlinSourceSetTreeClassifier.Name -> KotlinSourceSetTree(name)
        is KotlinSourceSetTreeClassifier.None -> null
        is SourceSetTreeClassifierWrapper -> classifier.classify(compilation)?.name?.let(::KotlinSourceSetTree)
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
internal var KotlinCompilation<*>.sourceSetTreeClassifier: KotlinSourceSetTreeClassifier by extrasLazyProperty {
    KotlinSourceSetTreeClassifier.Default
}


/**
 * Returns the [KotlinSourceSetTree] of a given [KotlinCompilation]:
 * Uses the [sourceSetTreeClassifier] under the hood.
 * See [KotlinSourceSetTreeClassifier]
 */
internal suspend fun KotlinSourceSetTree.Companion.orNull(compilation: KotlinCompilation<*>) =
    compilation.sourceSetTreeClassifier.classify(compilation)
