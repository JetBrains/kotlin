/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import java.io.File

/**
 * The hashes of the headers a cinterop task saw, by absolute path. Written after a successful run, read by the
 * next run to decide whether the headers changed. Build-local, so the format only has to be readable by this code.
 */
private val headerHashesSerializer = MapSerializer(String.serializer(), String.serializer())

internal fun File.writeHeaderHashes(hashes: Map<String, String>) {
    parentFile.mkdirs()
    writeText(KgpJson.prettyPrinted.encodeToString(headerHashesSerializer, hashes))
}

internal fun File.readHeaderHashes(): Map<String, String> =
    KgpJson.default.decodeFromString(headerHashesSerializer, readText())
