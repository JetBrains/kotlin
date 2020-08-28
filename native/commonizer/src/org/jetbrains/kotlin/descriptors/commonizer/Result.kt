/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import java.io.File

sealed class Result {
    object NothingToCommonize : Result()

    class Commonized(
        val modulesByTargets: Map<Target, Collection<ModuleResult>>
    ) : Result() {
        val sharedTarget: OutputTarget by lazy { modulesByTargets.keys.filterIsInstance<OutputTarget>().single() }
        val leafTargets: Set<InputTarget> by lazy { modulesByTargets.keys.filterIsInstance<InputTarget>().toSet() }
    }
}

sealed class ModuleResult {
    class Absent(val originalLocation: File) : ModuleResult()
    class Commonized(val module: ModuleDescriptor) : ModuleResult()
}
