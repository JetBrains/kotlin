/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.streams.asSequence

@Serializable
internal data class SetupFile(
    val properties: Map<String, String>,
)

private val json = Json { ignoreUnknownKeys = true }

// can't use decodeFromStream: https://github.com/Kotlin/kotlinx.serialization/issues/2218
internal fun parseSetupFile(inputStream: InputStream): SetupFile =
    json.decodeFromString(
        BufferedReader(InputStreamReader(inputStream)).lines().asSequence().joinToString("\n")
    )