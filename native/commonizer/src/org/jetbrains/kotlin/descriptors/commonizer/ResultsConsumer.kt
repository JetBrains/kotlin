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

    /**
     * Consume a single [ModuleResult] for the specified [CommonizerTarget].
     */
    fun consume(target: CommonizerTarget, moduleResult: ModuleResult)

    /**
     * Mark the specified [CommonizerTarget] as fully consumed.
     * It's forbidden to make subsequent [consume] calls for fully consumed targets.
     */
    fun targetConsumed(target: CommonizerTarget)

    /**
     * Notify that all results have been consumed.
     * It's forbidden to make any subsequent [consume] and [targetConsumed] calls after this call.
     */
    fun allConsumed(status: Status)
}
