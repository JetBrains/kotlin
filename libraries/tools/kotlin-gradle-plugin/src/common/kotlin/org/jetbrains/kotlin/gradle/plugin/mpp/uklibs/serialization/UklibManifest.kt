/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.ATTRIBUTES
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENTS
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENT_FILES
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENT_IDENTIFIER
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.UMANIFEST_VERSION

/**
 * The `umanifest` file of a uklib. It is published, so other Kotlin versions read it: keys may be added, but the
 * existing ones keep their names and types.
 */
@Serializable
internal class UklibManifest(
    @SerialName(FRAGMENTS)
    val fragments: List<Fragment>,
    @SerialName(UMANIFEST_VERSION)
    val manifestVersion: String,
) {
    @Serializable
    class Fragment(
        @SerialName(FRAGMENT_IDENTIFIER)
        val identifier: String,
        @SerialName(ATTRIBUTES)
        val targets: List<String>,
        /** Absent in a published uklib, where a fragment's files are the directory named after it. */
        @SerialName(FRAGMENT_FILES)
        val files: List<String>? = null,
    )

    companion object {
        /**
         * Fragments and their targets are sorted, so the same module always gives the same manifest. [withFiles] lists
         * each fragment's files, which a published uklib leaves out.
         */
        fun of(fragments: Collection<UklibFragment>, manifestVersion: String, withFiles: Boolean) = UklibManifest(
            fragments = fragments
                .sortedBy { it.identifier }
                .map { fragment ->
                    Fragment(
                        identifier = fragment.identifier,
                        targets = fragment.attributes.sorted(),
                        files = if (withFiles) fragment.files.map { it.path } else null,
                    )
                },
            manifestVersion = manifestVersion,
        )
    }
}

/** Only the version, so a manifest from a newer format is rejected before its other keys are read. */
@Serializable
private class UklibManifestVersion(
    @SerialName(UMANIFEST_VERSION)
    val manifestVersion: String,
)

internal fun UklibManifest.encodeToString(): String =
    KgpJson.prettyPrinted.encodeToString(UklibManifest.serializer(), this)

/** Accepts unquoted keys and values and comments, which the manifest reader has always taken. */
@OptIn(ExperimentalSerializationApi::class)
private val manifestReader = Json(KgpJson.default) {
    isLenient = true
    allowComments = true
}

/**
 * Reads the version first and lets [checkVersion] reject it before the rest of the manifest is decoded. A malformed
 * manifest throws a [kotlinx.serialization.SerializationException].
 */
internal fun decodeUklibManifest(text: String, checkVersion: (String) -> Unit): UklibManifest {
    val json = manifestReader.parseToJsonElement(text.removePrefix("\uFEFF"))
    checkVersion(manifestReader.decodeFromJsonElement(UklibManifestVersion.serializer(), json).manifestVersion)
    return manifestReader.decodeFromJsonElement(UklibManifest.serializer(), json)
}
