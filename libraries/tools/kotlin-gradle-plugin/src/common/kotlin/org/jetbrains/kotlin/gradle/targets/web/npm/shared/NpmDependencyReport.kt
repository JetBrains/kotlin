/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import java.io.File

/**
 * npm dependencies of one JS or WasmJS compilation, named by its module name.
 * Internal format, so fields can change freely.
 */
@Serializable
internal data class NpmDependencyReport(
    val target: String,
    val compilation: String,
    val dependencies: Map<String, String> = emptyMap(),
    val devDependencies: Map<String, String> = emptyMap(),
    val peerDependencies: Map<String, String> = emptyMap(),
    val optionalDependencies: Map<String, String> = emptyMap(),
)

internal const val NPM_DEPENDENCY_REPORT_FILE_NAME = "npm-dep-report.json"

internal fun NpmDependencyReport.writeTo(file: File) {
    file.parentFile.mkdirs()
    file.writeText(KgpJson.prettyPrinted.encodeToString(NpmDependencyReport.serializer(), this))
}

internal fun readNpmDependencyReport(file: File): NpmDependencyReport =
    KgpJson.default.decodeFromString(NpmDependencyReport.serializer(), file.readText())
