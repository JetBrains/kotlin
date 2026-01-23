/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_COMPILER_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_IR_SIGNATURE_VERSIONS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_METADATA_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File
import java.util.Properties

private val TRANSIENT_MANIFEST_PROPERTIES = listOf(
    KLIB_PROPERTY_ABI_VERSION,
    KLIB_PROPERTY_METADATA_VERSION,
    KLIB_PROPERTY_COMPILER_VERSION,
    KLIB_PROPERTY_IR_SIGNATURE_VERSIONS
)

private const val SANITIZED_VALUE_STUB = "<value sanitized for test data stability>"
private const val SANITIZED_TEST_RUN_TARGET = "<test-run-target>"

fun readManifestAndSanitize(klibDir: File, singleTargetInManifestToBeReplacedByTheAlias: KonanTarget?): String {
    val manifestFile = File(klibDir, "default/manifest")
    assertTrue(manifestFile.exists()) { "File does not exist: $manifestFile" }

    val manifestProperties = manifestFile.bufferedReader().use { reader -> Properties().apply { load(reader) } }
    return sanitizeManifest(
        manifestProperties,
        singleTargetInManifestToBeReplacedByTheAlias
    ).joinToString(separator = "\n") { (key, value) -> "$key = $value" }
}

private fun sanitizeManifest(
    original: Properties,
    singleTargetInManifestToBeReplacedByTheAlias: KonanTarget?
): List<Pair<String, String>> {
    // intentionally not using Properties as output to guarantee stable order of properties
    val result = mutableListOf<Pair<String, String>>()
    original.entries.forEach {
        val key = it.key as String
        val value = it.value as String

        val sanitizedValue = when (key) {
            in TRANSIENT_MANIFEST_PROPERTIES -> SANITIZED_VALUE_STUB

            KLIB_PROPERTY_NATIVE_TARGETS -> {
                val singleTargetPresentInManifest = value.split(" ").singleOrNull()
                if (!singleTargetPresentInManifest.isNullOrEmpty() && singleTargetInManifestToBeReplacedByTheAlias != null) {
                    SANITIZED_TEST_RUN_TARGET
                } else {
                    value
                }
            }

            else -> value
        }
        result += key to sanitizedValue
    }

    return result.sortedBy { it.first }
}
