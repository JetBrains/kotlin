/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.utils

import org.jetbrains.kotlin.project.model.KotlinModule
import org.jetbrains.kotlin.project.model.KotlinModuleFragment
import org.jetbrains.kotlin.project.model.KotlinModuleVariant
import org.jetbrains.kotlin.project.model.refinesClosure

fun KotlinModule.variantsContainingFragment(fragment: KotlinModuleFragment): Iterable<KotlinModuleVariant> =
    variants.filter { fragment in it.refinesClosure }

fun KotlinModule.findRefiningFragments(fragment: KotlinModuleFragment): Iterable<KotlinModuleFragment> {
    val refining = mutableSetOf<KotlinModuleFragment>()
    val notRefining = mutableSetOf<KotlinModuleFragment>()

    fun isRefining(other: KotlinModuleFragment): Boolean = when {
        other in refining -> true
        other in notRefining -> false
        fragment in other.directRefinesDependencies -> true.also { refining.add(other) }
        fragment.directRefinesDependencies.any { isRefining(it) } -> true.also { refining.add(other) }
        else -> false.also { notRefining.add(other) }
    }

    return fragments.filter(::isRefining)
}