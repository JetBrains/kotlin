/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor

sealed class CommonizationResult

object NothingToCommonize : CommonizationResult()

class CommonizationPerformed(
    val modulesByTargets: Map<Target, Collection<ModuleDescriptor>>
) : CommonizationResult() {
    val commonTarget: OutputTarget by lazy {
        modulesByTargets.keys.filterIsInstance<OutputTarget>().single()
    }

    val concreteTargets: Set<InputTarget> by lazy {
        modulesByTargets.keys.filterIsInstance<InputTarget>().toSet()
    }
}
