/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization

import com.google.gson.Gson
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.ATTRIBUTES
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENTS
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENT_IDENTIFIER
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.MAXIMUM_COMPATIBLE_UMANIFEST_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.UMANIFEST_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.UMANIFEST_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibModule
import java.io.File

internal data class IncompatibleUklibVersion(
    val uklibDirectory: File,
    val uklibVersion: String,
    val maximumCompatibleVersion: String,
) : IllegalStateException("Can't read Uklib at path $uklibDirectory with version ${uklibVersion}, maximum compatible version: $maximumCompatibleVersion")

internal fun deserializeUklibFromDirectory(directory: File): Uklib {
    val umanifest = directory.resolve(UMANIFEST_FILE_NAME)
    if (!umanifest.exists()) error("Can't deserialize Uklib from ${directory} because $UMANIFEST_FILE_NAME doesn't exist")
    @Suppress("UNCHECKED_CAST")
    val json = Gson().fromJson(umanifest.reader(), Map::class.java) as Map<String, Any>

    val manifestVersion = json.property<String>(UMANIFEST_VERSION)
    assertCanReadUManifest(manifestVersion, directory)

    val fragments = (json.property<List<Map<String, Any>>>(FRAGMENTS)).map { fragment ->
        val fragmentIdentifier = fragment.property<String>(FRAGMENT_IDENTIFIER)
        UklibFragment(
            identifier = fragmentIdentifier,
            attributes = fragment.property<List<String>>(ATTRIBUTES).toSet(),
            file = {
                directory.resolve(fragmentIdentifier)
            }
        )
    }.toSet()

    return Uklib(
        module = UklibModule(
            fragments = fragments,
        ),
        manifestVersion = manifestVersion,
    )
}

private inline fun <reified T> Map<String, Any>.property(named: String): T {
    if (!containsKey(named)) {
        error(
            """
            Json object missing required property $named
            $this
            """.trimIndent()
        )
    }
    val value = get(named)
    if (value !is T) {
        error(
            """
            Couldn't cast $value to type ${T::class}:                    
            $this
            """.trimIndent()
        )
    }
    return value
}

private fun assertCanReadUManifest(manifestVersion: String, directory: File) {
    if (manifestVersion != MAXIMUM_COMPATIBLE_UMANIFEST_VERSION) throw IncompatibleUklibVersion(
        directory,
        manifestVersion,
        MAXIMUM_COMPATIBLE_UMANIFEST_VERSION
    )
}