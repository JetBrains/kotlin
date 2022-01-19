/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.attributes.AttributeContainer

/* Internal abbreviation */
internal typealias FragmentAttributes<T> = KotlinGradleFragmentConfigurationAttributes<T>

fun interface KotlinGradleFragmentConfigurationAttributes<in T : KotlinGradleFragment> {
    fun AttributeContainer.setAttributes(fragment: T)

    companion object {
        val None = FragmentAttributes<KotlinGradleFragment> {}
    }
}

fun <T : KotlinGradleFragment> AttributeContainer.attributes(attributes: FragmentAttributes<T>, fragment: T) = with(attributes) {
    setAttributes(fragment)
}

operator fun <T : KotlinGradleFragment> FragmentAttributes<T>.plus(other: FragmentAttributes<T>): FragmentAttributes<T> {
    if (this is CompositeFragmentAttributes && other is CompositeFragmentAttributes) {
        return CompositeFragmentAttributes(this.children + other.children)
    }

    if (this === FragmentAttributes.None) return other
    if (other === FragmentAttributes.None) return this

    if (this is CompositeFragmentAttributes) {
        return CompositeFragmentAttributes(this.children + other)
    }

    if (other is CompositeFragmentAttributes) {
        return CompositeFragmentAttributes(listOf(this) + other.children)
    }

    return CompositeFragmentAttributes(listOf(this, other))
}

internal class CompositeFragmentAttributes<in T : KotlinGradleFragment>(val children: List<FragmentAttributes<T>>) :
    FragmentAttributes<T> {
    override fun AttributeContainer.setAttributes(fragment: T) {
        children.forEach { attribute -> attributes(attribute, fragment) }
    }
}
