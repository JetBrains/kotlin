/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.utils

import org.jetbrains.kotlin.project.model.KotlinModule
import org.jetbrains.kotlin.project.model.KotlinModuleFragment
import org.jetbrains.kotlin.project.model.KotlinModuleVariant
import org.jetbrains.kotlin.project.model.withRefinesClosure
import org.jetbrains.kotlin.tooling.core.closure

fun KotlinModule.variantsContainingFragment(fragment: KotlinModuleFragment): Iterable<KotlinModuleVariant> =
    variants.filter { variant -> fragment in variant.withRefinesClosure }

fun KotlinModule.findRefiningFragments(fragment: KotlinModuleFragment): Iterable<KotlinModuleFragment> {
    return fragment.closure { seedFragment ->
        fragments.filter { otherFragment -> seedFragment in otherFragment.directRefinesDependencies }
    }
}
