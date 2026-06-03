/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import java.io.File

@Deprecated("Unused string constant. Scheduled for removal in Kotlin 2.6.", ReplaceWith(""""js""""))
const val JS = "js"

@Deprecated("Unused string constant. Scheduled for removal in Kotlin 2.6.", ReplaceWith(""""mjs""""))
const val MJS = "mjs"

@Deprecated("Unused string constant. Scheduled for removal in Kotlin 2.6.", ReplaceWith(""""wasm""""))
const val WASM = "wasm"

@Deprecated("Unused string constant. Scheduled for removal in Kotlin 2.6.", ReplaceWith(""""js.map""""))
const val JS_MAP = "js.map"

@Deprecated("Unused string constant. Scheduled for removal in Kotlin 2.6.", ReplaceWith(""""meta.js""""))
const val META_JS = "meta.js"

@Deprecated("Unused string constant. Scheduled for removal in Kotlin 2.6.", ReplaceWith(""""html""""))
const val HTML = "html"

internal fun writeWasmUnitTestRunner(workingDir: File, compiledFile: File): File {
    val static = workingDir.resolve("static").also {
        it.mkdirs()
    }

    val testRunnerFile = static.resolve("runUnitTests.mjs")
    testRunnerFile.writeText(
        """
        import * as exports from './${compiledFile.relativeTo(static).invariantSeparatorsPath}';
        exports["startUnitTests"]?.();
        """.trimIndent()
    )
    return testRunnerFile
}

/**
 * Determines the appropriate variant (JavaScript or WebAssembly) to use based on the compilation configuration.
 *
 * @param jsVariant The variant to be used if the target is JavaScript.
 * @param wasmVariant The variant to be used if the target is WebAssembly.
 * @return The appropriate variant (either the result of `jsVariant` or `wasmVariant`), depending on the compilation configuration.
 */
internal fun <T> KotlinJsIrCompilation.webTargetVariant(
    jsVariant: T,
    wasmVariant: T,
): T = target.webTargetVariant(jsVariant, wasmVariant)

/**
 * Determines the appropriate variant (JavaScript or WebAssembly) to use based on the compilation configuration.
 *
 * @param jsVariant A lambda that returns the JavaScript-specific variant.
 * @param wasmVariant A lambda that returns the WebAssembly-specific variant.
 * @return The appropriate variant (either the result of `jsVariant` or `wasmVariant`), depending on the compilation configuration.
 */
internal fun <T> KotlinJsIrCompilation.webTargetVariant(
    jsVariant: () -> T,
    wasmVariant: () -> T,
): T = target.webTargetVariant(jsVariant, wasmVariant)

/**
 * Determines the appropriate variant (JavaScript or WebAssembly) to use based on the target configuration.
 *
 * @param jsVariant A lambda that returns the JavaScript-specific variant.
 * @param wasmVariant A lambda that returns the WebAssembly-specific variant.
 * @return The appropriate variant (either the result of `jsVariant` or `wasmVariant`), depending on the target configuration.
 */
internal fun <T> KotlinJsIrTarget.webTargetVariant(
    jsVariant: () -> T,
    wasmVariant: () -> T,
): T = if (wasmTargetType == null) {
    jsVariant()
} else {
    wasmVariant()
}

/**
 * Determines the appropriate variant (JavaScript or WebAssembly) to use based on the target configuration.
 *
 * @param jsVariant The variant to be used if the target is JavaScript.
 * @param wasmVariant The variant to be used if the target is WebAssembly.
 * @return The appropriate variant (either the result of `jsVariant` or `wasmVariant`), depending on the target configuration.
 */
internal fun <T> KotlinJsIrTarget.webTargetVariant(
    jsVariant: T,
    wasmVariant: T,
): T = if (wasmTargetType == null) {
    jsVariant
} else {
    wasmVariant
}

/**
 * Default JSON emitter — converts arbitrary Map/List/primitive trees to pretty-printed JSON.
 */
private val prettyJson = Json { prettyPrint = true }

internal fun json(obj: Any): String =
    prettyJson.encodeToString(JsonElement.serializer(), anyToJsonElement(obj))

private fun anyToJsonElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is String -> JsonPrimitive(value)
    is Map<*, *> -> buildJsonObject {
        value.forEach { (k, v) -> put(k.toString(), anyToJsonElement(v)) }
    }
    is Iterable<*> -> buildJsonArray {
        value.forEach { add(anyToJsonElement(it)) }
    }
    is Array<*> -> buildJsonArray {
        value.forEach { add(anyToJsonElement(it)) }
    }
    else -> JsonPrimitive(value.toString())
}
