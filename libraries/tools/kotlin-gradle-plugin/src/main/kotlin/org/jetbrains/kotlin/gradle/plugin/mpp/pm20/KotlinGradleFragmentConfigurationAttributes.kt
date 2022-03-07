/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer

/* Internal abbreviation */
internal typealias FragmentAttributes<T> = KotlinGradleFragmentConfigurationAttributes<T>

interface KotlinGradleFragmentConfigurationAttributes<in T : KotlinGradleFragment> {

    fun setAttributes(attributes: AttributeContainer, fragment: T)

    object None : FragmentAttributes<KotlinGradleFragment> {
        override fun setAttributes(attributes: AttributeContainer, fragment: KotlinGradleFragment) = Unit
    }
}

class KotlinGradleFragmentConfigurationAttributesContext<T : KotlinGradleFragment> internal constructor(
    internal val attributes: AttributeContainer,
    val fragment: T,
) : AttributeContainer by attributes {
    val project: Project get() = fragment.project

    inline fun <reified T : Named> named(name: String): T = project.objects.named(T::class.java, name)

    inline fun <reified T : Named> namedAttribute(key: Attribute<T>, name: String) = apply { attribute(key, named(name)) }

    override fun <K : Any> attribute(key: Attribute<K>, value: K): KotlinGradleFragmentConfigurationAttributesContext<T> = apply {
        attributes.attribute(key, value)
    }
}

@Suppress("FunctionName")
fun <T : KotlinGradleFragment> FragmentAttributes(
    setAttributes: KotlinGradleFragmentConfigurationAttributesContext<T>.() -> Unit
): KotlinGradleFragmentConfigurationAttributes<T> {
    return object : KotlinGradleFragmentConfigurationAttributes<T> {
        override fun setAttributes(attributes: AttributeContainer, fragment: T) {
            val context = KotlinGradleFragmentConfigurationAttributesContext(attributes, fragment)
            context.setAttributes()
        }
    }
}

fun <T : KotlinGradleFragment> AttributeContainer.apply(
    attributes: KotlinGradleFragmentConfigurationAttributes<T>, fragment: T
) {
    attributes.setAttributes(this, fragment)
}

operator fun <T : KotlinGradleFragment> FragmentAttributes<T>.plus(other: FragmentAttributes<T>): FragmentAttributes<T> {
    if (this === KotlinGradleFragmentConfigurationAttributes.None) return other
    if (other === KotlinGradleFragmentConfigurationAttributes.None) return this

    if (this is CompositeFragmentAttributes && other is CompositeFragmentAttributes) {
        return CompositeFragmentAttributes(this.children + other.children)
    }

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
    override fun setAttributes(attributes: AttributeContainer, fragment: T) {
        children.forEach { child -> child.setAttributes(attributes, fragment) }
    }
}
