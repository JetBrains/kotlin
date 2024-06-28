/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.commonizer.metadata.utils.MetadataDeclarationsComparator
import org.jetbrains.kotlin.commonizer.metadata.utils.MetadataDeclarationsComparator.*
import org.jetbrains.kotlin.commonizer.metadata.utils.SerializedMetadataLibraryProvider
import org.jetbrains.kotlin.library.SerializedMetadata
import java.io.File
import kotlin.test.fail

fun assertIsDirectory(file: File) {
    if (!file.isDirectory)
        fail("Not a directory: $file")
}

fun assertModulesAreEqual(reference: SerializedMetadata, generated: SerializedMetadata, target: CommonizerTarget) {
    val referenceModule = KlibModuleMetadata.read(SerializedMetadataLibraryProvider(reference))
    val generatedModule = KlibModuleMetadata.read(SerializedMetadataLibraryProvider(generated))

    when (val result = MetadataDeclarationsComparator.compare(referenceModule, generatedModule)) {
        is Result.Success -> Unit
        is Result.Failure -> {
            val mismatches = result.mismatches
                .filter(FILTER_OUT_ACCEPTABLE_MISMATCHES)
                .sortedBy { it::class.java.simpleName + "_" + it.kind }

            if (mismatches.isEmpty()) return

            val digitCount = mismatches.size.toString().length

            val failureMessage = buildString {
                appendLine(
                    "${mismatches.size} mismatches found while comparing reference module ${referenceModule.name} (A) " +
                            "and generated module ${generatedModule.name} (B) for target ${target.identityString}:"
                )
                mismatches.forEachIndexed { index, mismatch ->
                    appendLine((index + 1).toString().padStart(digitCount, ' ') + ". " + mismatch)
                }
            }

            fail(failureMessage)
        }
    }
}

private val FILTER_OUT_ACCEPTABLE_MISMATCHES: (Mismatch) -> Boolean = { mismatch ->
    var isAcceptableMismatch = false // don't filter it out by default

    when (mismatch) {
        is Mismatch.MissingEntity -> when (mismatch.kind) {
            EntityKind.TypeKind.INLINE_CLASS_UNDERLYING, EntityKind.InlineClassUnderlyingProperty -> {
                isAcceptableMismatch = true
            }
            EntityKind.TypeKind.ABBREVIATED -> {
                val usefulPath = mismatch.path
                    .dropWhile { it !is PathElement.Package }
                    .drop(1)

                if (mismatch.missingInA) {
                    if (usefulPath.size == 2
                        && usefulPath[0] is PathElement.TypeAlias
                        && (usefulPath[1] as? PathElement.Type)?.kind == EntityKind.TypeKind.EXPANDED
                    ) {
                        // extra abbreviated type appeared in commonized declaration, it's OK
                        isAcceptableMismatch = true
                    } else {
                        /* The initial intention implemented in d6961a6e is unclear and needs to be reviewed */
                        /* Only known test case that enters this branch is `test KT-51686` */
                        println("[WARNING] Potentially unacceptable mismatch found $mismatch")
                        isAcceptableMismatch = true
                    }
                } else /*if (mismatch.missingInB)*/ {
                    if (usefulPath.size > 2
                        && usefulPath.any { (it as? PathElement.Type)?.kind == EntityKind.TypeKind.RETURN }
                        && usefulPath[usefulPath.size - 2] is PathElement.TypeArgument
                        && (usefulPath[usefulPath.size - 1] as? PathElement.Type)?.kind == EntityKind.TypeKind.TYPE_ARGUMENT
                    ) {
                        // extra abbreviated type gone in type argument of commonized declaration, it's OK
                        isAcceptableMismatch = true
                    }
                }
            }
            else -> Unit
        }
        is Mismatch.DifferentValues -> when (mismatch.kind) {
            EntityKind.FlagKind.REGULAR, EntityKind.FlagKind.GETTER, EntityKind.FlagKind.SETTER -> {
                if (mismatch.name == "HAS_ANNOTATIONS"
                    && mismatch.valueA == true
                    && mismatch.valueB == false
                    && (mismatch.path.last() as? PathElement.Property)?.propertyA?.annotations.isNullOrEmpty()
                ) {
                    // backing or delegate field annotations were not serialized (KT-44625) but the corresponding flag was raised, it's OK
                    isAcceptableMismatch = true
                }
            }
            else -> Unit
        }
    }

    !isAcceptableMismatch
}
