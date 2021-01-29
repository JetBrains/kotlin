/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.library.SerializedMetadata
import java.io.File

sealed class CommonizerResult {
    object NothingToDo : CommonizerResult()

    class Done(
        val modulesByTargets: Map<CommonizerTarget, Collection<ModuleResult>>
    ) : CommonizerResult() {
        val sharedTarget: SharedTarget by lazy { modulesByTargets.keys.filterIsInstance<SharedTarget>().single() }
        val leafTargets: Set<LeafTarget> by lazy { modulesByTargets.keys.filterIsInstance<LeafTarget>().toSet() }
    }
}

sealed class ModuleResult {
    abstract val libraryName: String

    class Missing(val originalLocation: File) : ModuleResult() {
        override val libraryName: String get() = originalLocation.name
    }

    class Commonized(override val libraryName: String, val metadata: SerializedMetadata) : ModuleResult()
}
