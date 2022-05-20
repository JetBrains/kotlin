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

interface GradleKpmConfigurationAttributesSetup<in T : GradleKpmFragment> {

    fun setupAttributes(attributes: AttributeContainer, fragment: T)

    object None : GradleKpmConfigurationAttributesSetup<GradleKpmFragment> {
        override fun setupAttributes(attributes: AttributeContainer, fragment: GradleKpmFragment) = Unit
    }
}

class GradleKpmConfigurationAttributesSetupContext<T : GradleKpmFragment> internal constructor(
    internal val attributes: AttributeContainer,
    val fragment: T,
) : AttributeContainer by attributes {
    val project: Project get() = fragment.project

    inline fun <reified T : Named> named(name: String): T = project.objects.named(T::class.java, name)

    inline fun <reified T : Named> namedAttribute(key: Attribute<T>, name: String) = apply { attribute(key, named(name)) }

    override fun <K : Any> attribute(key: Attribute<K>, value: K): GradleKpmConfigurationAttributesSetupContext<T> = apply {
        attributes.attribute(key, value)
    }
}

@Suppress("FunctionName")
fun <T : GradleKpmFragment> GradleKpmConfigurationAttributesSetup(
    setAttributes: GradleKpmConfigurationAttributesSetupContext<T>.() -> Unit
): GradleKpmConfigurationAttributesSetup<T> {
    return object : GradleKpmConfigurationAttributesSetup<T> {
        override fun setupAttributes(attributes: AttributeContainer, fragment: T) {
            val context = GradleKpmConfigurationAttributesSetupContext(attributes, fragment)
            context.setAttributes()
        }
    }
}

fun <T : GradleKpmFragment> AttributeContainer.apply(
    attributes: GradleKpmConfigurationAttributesSetup<T>, fragment: T
) {
    attributes.setupAttributes(this, fragment)
}

operator fun <T : GradleKpmFragment> GradleKpmConfigurationAttributesSetup<T>.plus(other: GradleKpmConfigurationAttributesSetup<T>): GradleKpmConfigurationAttributesSetup<T> {
    if (this === GradleKpmConfigurationAttributesSetup.None) return other
    if (other === GradleKpmConfigurationAttributesSetup.None) return this

    if (this is GradleKpmCompositeConfigurationAttributesSetup && other is GradleKpmCompositeConfigurationAttributesSetup) {
        return GradleKpmCompositeConfigurationAttributesSetup(this.children + other.children)
    }

    if (this is GradleKpmCompositeConfigurationAttributesSetup) {
        return GradleKpmCompositeConfigurationAttributesSetup(this.children + other)
    }

    if (other is GradleKpmCompositeConfigurationAttributesSetup) {
        return GradleKpmCompositeConfigurationAttributesSetup(listOf(this) + other.children)
    }

    return GradleKpmCompositeConfigurationAttributesSetup(listOf(this, other))
}

internal class GradleKpmCompositeConfigurationAttributesSetup<in T : GradleKpmFragment>(val children: List<GradleKpmConfigurationAttributesSetup<T>>) :
    GradleKpmConfigurationAttributesSetup<T> {
    override fun setupAttributes(attributes: AttributeContainer, fragment: T) {
        children.forEach { child -> child.setupAttributes(attributes, fragment) }
    }
}
