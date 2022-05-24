/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.utils

import org.jetbrains.kotlin.project.model.KpmModule
import org.jetbrains.kotlin.project.model.KpmFragment
import org.jetbrains.kotlin.project.model.KpmVariant
import org.jetbrains.kotlin.tooling.core.closure

fun KpmModule.variantsContainingFragment(fragment: KpmFragment): Iterable<KpmVariant> =
    variants.filter { variant -> fragment in variant.withRefinesClosure }

fun KpmModule.findRefiningFragments(fragment: KpmFragment): Iterable<KpmFragment> {
    return fragment.closure { seedFragment ->
        fragments.filter { otherFragment -> seedFragment in otherFragment.declaredRefinesDependencies }
    }
}
