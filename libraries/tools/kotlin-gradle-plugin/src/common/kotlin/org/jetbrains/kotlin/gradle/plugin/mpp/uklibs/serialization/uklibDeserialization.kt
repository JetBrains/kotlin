/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization

import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.MAXIMUM_COMPATIBLE_UMANIFEST_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.UMANIFEST_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibModule
import java.io.File

internal data class IncompatibleUklibVersion(
    val uklibDirectory: File,
    val uklibVersion: String,
    val maximumCompatibleVersion: String,
) : IllegalStateException("Can't read Uklib at path $uklibDirectory with version ${uklibVersion}, maximum compatible version: $maximumCompatibleVersion")

internal fun deserializeUklibFromDirectory(
    directory: File,
): Uklib {
    val umanifest = directory.resolve(UMANIFEST_FILE_NAME)
    if (!umanifest.exists()) error("Can't deserialize Uklib from ${directory} because $UMANIFEST_FILE_NAME doesn't exist")
    return umanifest.reader().use { umanifestStream ->
        val manifest = KgpJson.default.decodeFromString(
            UklibManifest.serializer(),
            umanifestStream.readText()
        )

        assertCanReadUManifest(manifest.manifestVersion, directory)

        val fragments = manifest.fragments.map { fragment ->
            UklibFragment(
                identifier = fragment.identifier,
                attributes = fragment.targets.toSet(),
                files = if (fragment.files != null)
                    fragment.files.map(::File)
                else listOf(directory.resolve(fragment.identifier))
            )
        }.toSet()

        Uklib(
            module = UklibModule(
                fragments = fragments,
            ),
            manifestVersion = manifest.manifestVersion,
        )
    }
}

private fun assertCanReadUManifest(manifestVersion: String, directory: File) {
    if (manifestVersion != MAXIMUM_COMPATIBLE_UMANIFEST_VERSION) throw IncompatibleUklibVersion(
        directory,
        manifestVersion,
        MAXIMUM_COMPATIBLE_UMANIFEST_VERSION
    )
}
