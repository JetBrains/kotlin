/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation

/* Internal abbreviation */
internal typealias FragmentConfigurationRelation = KotlinGradleFragmentConfigurationRelation

interface KotlinGradleFragmentConfigurationRelation {
    fun setExtendsFrom(configuration: Configuration, context: KotlinGradleFragmentConfigurationContext)

    object None : KotlinGradleFragmentConfigurationRelation {
        override fun setExtendsFrom(configuration: Configuration, context: KotlinGradleFragmentConfigurationContext) = Unit
    }
}

operator fun FragmentConfigurationRelation.plus(other: FragmentConfigurationRelation): FragmentConfigurationRelation {
    if (this === KotlinGradleFragmentConfigurationRelation.None) return other
    if (other === KotlinGradleFragmentConfigurationRelation.None) return this

    if (this is CompositeFragmentConfigurationRelation && other is CompositeFragmentConfigurationRelation) {
        return CompositeFragmentConfigurationRelation(this.children + other.children)
    }

    if (this is CompositeFragmentConfigurationRelation) {
        return CompositeFragmentConfigurationRelation(this.children + other)
    }

    if (other is CompositeFragmentConfigurationRelation) {
        return CompositeFragmentConfigurationRelation(listOf(this) + other.children)
    }

    return CompositeFragmentConfigurationRelation(listOf(this, other))
}

internal class CompositeFragmentConfigurationRelation(val children: List<FragmentConfigurationRelation>) :
    FragmentConfigurationRelation {

    override fun setExtendsFrom(configuration: Configuration, context: KotlinGradleFragmentConfigurationContext) {
        children.forEach { child -> child.setExtendsFrom(configuration, context) }
    }
}

class KotlinGradleFragmentConfigurationRelationContext internal constructor(
    val configuration: Configuration,
    context: KotlinGradleFragmentConfigurationContext
) : KotlinGradleFragmentConfigurationContext by context {
    fun extendsFrom(configuration: Configuration) = this.configuration.extendsFrom(configuration)
    fun extendsFrom(configuration: String) = project.addExtendsFromRelation(this.configuration.name, configuration)
}

fun FragmentConfigurationRelation(
    setExtendsFrom: KotlinGradleFragmentConfigurationRelationContext.() -> Unit
): KotlinGradleFragmentConfigurationRelation = object : KotlinGradleFragmentConfigurationRelation {
    override fun setExtendsFrom(configuration: Configuration, context: KotlinGradleFragmentConfigurationContext) {
        val relationContext = KotlinGradleFragmentConfigurationRelationContext(configuration, context)
        relationContext.setExtendsFrom()
    }
}
