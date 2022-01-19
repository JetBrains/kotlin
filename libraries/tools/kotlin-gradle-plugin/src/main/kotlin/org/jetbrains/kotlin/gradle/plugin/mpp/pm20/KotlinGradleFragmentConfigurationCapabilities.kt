/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.capabilities.Capability

/* Internal abbreviation */
internal typealias FragmentCapabilities<T> = KotlinGradleFragmentConfigurationCapabilities<T>

fun interface KotlinGradleFragmentConfigurationCapabilities<in T : KotlinGradleFragment> {
    interface Context {
        fun capability(notation: Any)
        val capabilities: List<Capability>
    }

    class ContextImpl internal constructor(
        private val configuration: Configuration
    ) : Context {
        override fun capability(notation: Any) = configuration.outgoing.capability(notation)
        override val capabilities: List<Capability> get() = configuration.outgoing.capabilities.orEmpty().toList()
    }

    fun Context.setCapabilities(fragment: T)

    companion object {
        val None = FragmentCapabilities<KotlinGradleFragment> {}
    }
}

fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationCapabilities<T>.onlyIfMadePublic(): KotlinGradleFragmentConfigurationCapabilities<T> {
    return KotlinGradleFragmentConfigurationCapabilities { fragment ->
        fragment.containingModule.ifMadePublic { capabilities(this@onlyIfMadePublic, fragment) }
    }
}

fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationCapabilities.Context.capabilities(
    capabilities: KotlinGradleFragmentConfigurationCapabilities<T>, fragment: T
) = with(capabilities) { setCapabilities(fragment) }

operator fun <T : KotlinGradleFragment> FragmentCapabilities<T>.plus(other: FragmentCapabilities<T>): FragmentCapabilities<T> {
    if (this is CompositeFragmentCapabilities && other is CompositeFragmentCapabilities) {
        return CompositeFragmentCapabilities(this.children + other.children)
    }

    if (this === FragmentCapabilities.None) return other
    if (other === FragmentCapabilities.None) return this

    if (this is CompositeFragmentCapabilities) {
        return CompositeFragmentCapabilities(this.children + other)
    }

    if (other is CompositeFragmentCapabilities) {
        return CompositeFragmentCapabilities(listOf(this) + other.children)
    }

    return CompositeFragmentCapabilities(listOf(this, other))
}

internal class CompositeFragmentCapabilities<in T : KotlinGradleFragment>(val children: List<FragmentCapabilities<T>>) :
    FragmentCapabilities<T> {
    override fun KotlinGradleFragmentConfigurationCapabilities.Context.setCapabilities(fragment: T) {
        children.forEach { capability -> capabilities(capability, fragment) }
    }
}
