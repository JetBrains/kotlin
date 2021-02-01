/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.library.SerializedMetadata
import java.io.File

interface ResultsConsumer {
    enum class Status { NOTHING_TO_DO, DONE }

    sealed class ModuleResult {
        abstract val libraryName: String

        class Missing(val originalLocation: File) : ModuleResult() {
            override val libraryName: String get() = originalLocation.name
        }

        class Commonized(override val libraryName: String, val metadata: SerializedMetadata) : ModuleResult()
    }

    fun consumeResults(target: CommonizerTarget, moduleResults: Collection<ModuleResult>)

    fun successfullyFinished(status: Status)
}
