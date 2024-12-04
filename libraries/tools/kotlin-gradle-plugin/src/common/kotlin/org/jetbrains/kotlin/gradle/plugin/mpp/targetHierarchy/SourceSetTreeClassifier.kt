/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.SourceSetTree
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinSourceSetTreeClassifier

@ExternalKotlinTargetApi
@Deprecated(
    "Use org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinSourceSetTreeClassifier instead",
    ReplaceWith("org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinSourceSetTreeClassifier"),
)
sealed class SourceSetTreeClassifier {

    @ExternalKotlinTargetApi
    object Default : SourceSetTreeClassifier() {
        override fun toString(): String = "Default"
    }

    @ExternalKotlinTargetApi
    object None : SourceSetTreeClassifier() {
        override fun toString(): String = "None"
    }

    @ExternalKotlinTargetApi
    data class Value(val tree: SourceSetTree) : SourceSetTreeClassifier()


    @ExternalKotlinTargetApi
    data class Name(val name: String) : SourceSetTreeClassifier()


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
