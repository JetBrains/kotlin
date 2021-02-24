/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.identityString
import org.jetbrains.kotlin.descriptors.commonizer.metadata.utils.MetadataDeclarationsComparator
import org.jetbrains.kotlin.descriptors.commonizer.metadata.utils.MetadataDeclarationsComparator.Mismatch
import org.jetbrains.kotlin.descriptors.commonizer.metadata.utils.MetadataDeclarationsComparator.Result
import org.jetbrains.kotlin.descriptors.commonizer.metadata.utils.SerializedMetadataLibraryProvider
import org.jetbrains.kotlin.library.SerializedMetadata
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.test.fail

fun assertIsDirectory(file: File) {
    if (!file.isDirectory)
        fail("Not a directory: $file")
}

@ExperimentalContracts
fun assertModulesAreEqual(reference: SerializedMetadata, generated: SerializedMetadata, target: CommonizerTarget) {
    val referenceModule = KlibModuleMetadata.read(SerializedMetadataLibraryProvider(reference))
    val generatedModule = KlibModuleMetadata.read(SerializedMetadataLibraryProvider(generated))

    when (val result = MetadataDeclarationsComparator.compare(referenceModule, generatedModule)) {
        is Result.Success -> Unit
        is Result.Failure -> {
            val mismatches = result.mismatches
                .filter(FILTER_OUR_ACCEPTABLE_MISMATCHES)
                .sortedBy { it::class.java.simpleName + "_" + it.kind }

            if (mismatches.isEmpty()) return

            val digitCount = mismatches.size.toString().length

            val failureMessage = buildString {
                appendLine("${mismatches.size} mismatches found while comparing reference module ${referenceModule.name} (A) and generated module ${generatedModule.name} (B) for target ${target.identityString}:")
                mismatches.forEachIndexed { index, mismatch ->
                    appendLine((index + 1).toString().padStart(digitCount, ' ') + ". " + mismatch)
                }
            }

            fail(failureMessage)
        }
    }
}

private val FILTER_OUR_ACCEPTABLE_MISMATCHES: (Mismatch) -> Boolean = { mismatch ->
    var isAcceptableMismatch = false // don't filter it out by default

    if (mismatch is Mismatch.MissingEntity) {
        if (mismatch.kind == "AbbreviatedType") {
            val usefulPath = mismatch.path
                .dropWhile { !it.startsWith("Package ") }
                .drop(1)
                .joinToString(" > ") { it.substringBefore(' ') }

            if (mismatch.missingInA) {
                if (usefulPath == "TypeAlias > ExpandedType") {
                    // extra abbreviated type appeared in commonized declaration, it's OK
                    isAcceptableMismatch = true
                }
            } else /*if (mismatch.missingInB)*/ {
                if ("> ReturnType >" in usefulPath && usefulPath.endsWith("TypeProjection > Type")) {
                    // extra abbreviated type gone in type argument of commonized declaration, it's OK
                    isAcceptableMismatch = true
                }
            }
        }
    }

    !isAcceptableMismatch
}
