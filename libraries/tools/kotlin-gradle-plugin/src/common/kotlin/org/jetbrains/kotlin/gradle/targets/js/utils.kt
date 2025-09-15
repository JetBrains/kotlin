/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import java.io.File
import org.jetbrains.kotlin.gradle.targets.js.internal.appendConfigsFromDir as appendConfigsFromDirInternal
import org.jetbrains.kotlin.gradle.targets.js.internal.calculateDirHash as calculateDirHashInternal
import org.jetbrains.kotlin.gradle.targets.js.internal.toHex as toHexInternal

@Deprecated(
    "Internal KGP utility. Scheduled for removal in Kotlin 2.4.",
    level = DeprecationLevel.ERROR
)
@Suppress("DeprecatedCallableAddReplaceWith")
fun Appendable.appendConfigsFromDir(confDir: File) {
    appendConfigsFromDirInternal(confDir)
}

@Deprecated(
    "Internal KGP utility. Scheduled for removal in Kotlin 2.4.",
    level = DeprecationLevel.ERROR
)
@Suppress("DeprecatedCallableAddReplaceWith")
fun ByteArray.toHex(): String =
    toHexInternal()

@Deprecated(
    "Internal KGP utility. Scheduled for removal in Kotlin 2.4.",
    level = DeprecationLevel.ERROR
)
@Suppress("DeprecatedCallableAddReplaceWith")
fun FileHasher.calculateDirHash(
    dir: File,
): String? =
    calculateDirHashInternal(dir)

const val JS = "js"
const val MJS = "mjs"
const val WASM = "wasm"
const val JS_MAP = "js.map"
const val META_JS = "meta.js"
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
