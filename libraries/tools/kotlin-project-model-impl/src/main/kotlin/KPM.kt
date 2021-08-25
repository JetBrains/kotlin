/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx

import org.jetbrains.kotlin.config.CompilerConfiguration

typealias FragmentId = String
typealias ModuleId = String

/**
 * Invetarise
 * my KPM classes and gradle ones,
 * find the differences and common parts
 */
data class KotlinModule(
    /**
     * Each KotlinModule have an unique identifier which is used for identification when publishing
     * TODO: can be generic
     */
    val id: String,

    /**
     * Fragments & Variants of a module.
     * Fragments form a Tree (or Forest?) through [refinements] map.
     */
    val fragments: Map<FragmentId, Fragment>,

    /**
     * Node at Key refines nodes from Value
     *
     * Some invariants are enforced here:
     * * Any [CommonFragment] must not refine a [Variant]
     * * Any [CommonFragment] must be refined by another [CommonFragment] or [Variant]
     *   -- this implies that [Variant] is always end-node in the refinement closure
     * * No circular references refines are not allowed.
     * * TBD: [Fragment] that refines another [Fragment] must have is-consistent relation as well
     */
    val refinements: Map<FragmentId, Set<FragmentId>>
) {
    /**
     * Node at Key gets refined by nodes from Value
     */
    val reverseRefinementsMap: Map<FragmentId, Set<FragmentId>> by lazy {
        val result = mutableMapOf<FragmentId, MutableSet<FragmentId>>()

        for ((refiner, parents) in refinements) {
            for (parent in parents) {
                result.getOrPut(parent) { mutableSetOf() }.add(refiner)
            }
        }

        result
    }

    val variants by lazy {
        @Suppress("UNCHECKED_CAST")
        fragments.filterValues { it is Variant } as Map<FragmentId, Variant>
    }

    /**
     * Returns a [Variant] by given fragment [id]
     * Throws an [Error] when no such fragment exists or found fragment isn't a [Variant]
     */
    fun variant(id: FragmentId): Variant {
        val fragment = fragments[id] ?: error("Fragment $id not found")
        if (fragment !is Variant) error("Requested Fragment $id is not a Variant")

        return fragment
    }
}

sealed class Fragment {
    abstract val id: FragmentId

    /**
     * Various language settings
     */
    abstract val settings: Map<String, LanguageSetting>

    /**
     * A fragment can depend on a set of [ModuleId]'s
     * Which later is expanded into set of [FragmentDependency]'s
     */
    abstract val moduleDependencies: Set<ModuleId>
}

data class CommonFragment(
    override val id: FragmentId,
    override val settings: Map<String, LanguageSetting>,
    override val moduleDependencies: Set<ModuleId>
) : Fragment()

data class Variant(
    override val id: FragmentId,
    override val settings: Map<String, LanguageSetting>,
    val attributes: Map<Attribute.Key, Attribute>,
    override val moduleDependencies: Set<ModuleId>,
) : Fragment() {
    /**
     * Platform describes a target platform
     */
    val platform: Platform by lazy {
        // TODO: too many assumptions, can't we make it clear and type safe this?
        val platformAttribute = attributes[Platforms] ?: error("Platforms attribute not found")
        platformAttribute as Platforms

        platformAttribute.platforms.first()
    }
}

enum class Platform {
    JVM, JS, Native
}
