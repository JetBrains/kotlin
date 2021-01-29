/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerResult
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.metadata.utils.MetadataDeclarationsComparator
import org.jetbrains.kotlin.descriptors.commonizer.metadata.utils.MetadataDeclarationsComparator.Result
import org.jetbrains.kotlin.descriptors.commonizer.metadata.utils.SerializedMetadataLibraryProvider
import org.jetbrains.kotlin.library.SerializedMetadata
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.fail

fun assertIsDirectory(file: File) {
    if (!file.isDirectory)
        fail("Not a directory: $file")
}

@ExperimentalContracts
fun assertCommonizationPerformed(result: CommonizerResult) {
    contract {
        returns() implies (result is CommonizerResult.Done)
    }

    if (result !is CommonizerResult.Done)
        fail("$result is not instance of ${CommonizerResult.Done::class}")
}

@ExperimentalContracts
fun assertModulesAreEqual(expected: SerializedMetadata, actual: SerializedMetadata, target: CommonizerTarget) {
    val expectedModule = with(expected) { KlibModuleMetadata.read(SerializedMetadataLibraryProvider(module, fragments, fragmentNames)) }
    val actualModule = with(actual) { KlibModuleMetadata.read(SerializedMetadataLibraryProvider(module, fragments, fragmentNames)) }

    when (val result = MetadataDeclarationsComparator().compare(expectedModule, actualModule)) {
        is Result.Success -> Unit
        is Result.Failure -> {
            val mismatches = result.mismatches.sortedBy { it::class.java.simpleName + "_" + it.kind }
            val digitCount = mismatches.size.toString().length

            val failureMessage = buildString {
                appendLine("${mismatches.size} mismatches found while comparing module ${expectedModule.name} and ${actualModule.name} for target ${target.prettyName}:")
                mismatches.forEachIndexed { index, mismatch ->
                    appendLine((index + 1).toString().padStart(digitCount, ' ') + ". " + mismatch)
                }
            }

            fail(failureMessage)
        }
    }
}
