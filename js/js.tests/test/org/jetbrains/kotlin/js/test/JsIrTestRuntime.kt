/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import java.io.File

private const val runtimeDir = "js/js.translator/testData/out/irBox"

enum class JsIrTestRuntime(
    val sources: List<String>,
    val path: String
) {
    // Includes all built-ins and reduced subset of stdlib
    DEFAULT(
        sources = reducedRuntimeSources,
        path = "$runtimeDir/testRuntime.js"
    ),

    // Includes almost full stdlib
    FULL(
        sources = fullRuntimeSources,
        path = "$runtimeDir/testRuntimeFull.js"
    )
}

private val fullRuntimeSources = listOfKtFilesFrom(
    "core/builtins/src/kotlin",
    "libraries/stdlib/common/src",
    "libraries/stdlib/src/kotlin/",
    "libraries/stdlib/js/src/kotlin",
    "libraries/stdlib/js/src/generated",
    "libraries/stdlib/js/irRuntime",
    "libraries/stdlib/js/runtime",
    "libraries/stdlib/unsigned",

    "core/builtins/native/kotlin/Annotation.kt",
    "core/builtins/native/kotlin/Number.kt",
    "core/builtins/native/kotlin/Comparable.kt",
    "core/builtins/native/kotlin/Collections.kt",
    "core/builtins/native/kotlin/Iterator.kt",
    "core/builtins/native/kotlin/CharSequence.kt",

    "core/builtins/src/kotlin/Unit.kt",

    BasicBoxTest.COMMON_FILES_DIR_PATH
) - listOfKtFilesFrom(
    "libraries/stdlib/common/src/kotlin/JvmAnnotationsH.kt",
    "libraries/stdlib/src/kotlin/annotations/Multiplatform.kt",
    "libraries/stdlib/common/src/kotlin/NativeAnnotationsH.kt",

    // TODO: Support Int.pow
    "libraries/stdlib/js/src/kotlin/random/PlatformRandom.kt",

    // Fails with: EXPERIMENTAL_IS_NOT_ENABLED
    "libraries/stdlib/common/src/kotlin/annotations/Annotations.kt",

    // Conflicts with libraries/stdlib/js/src/kotlin/annotations.kt
    "libraries/stdlib/js/runtime/hacks.kt",

    // TODO: Reuse in IR BE
    "libraries/stdlib/js/runtime/Enum.kt",

    // JS-specific optimized version of emptyArray() already defined
    "core/builtins/src/kotlin/ArrayIntrinsics.kt",

    // Unnecessary for now
    "libraries/stdlib/js/src/kotlin/dom",
    "libraries/stdlib/js/src/kotlin/browser",

    // TODO: fix compilation issues in arrayPlusCollection
    // Replaced with irRuntime/kotlinHacks.kt
    "libraries/stdlib/js/src/kotlin/kotlin.kt",

    "libraries/stdlib/js/src/kotlin/currentBeMisc.kt",

    // IR BE has its own generated sources
    "libraries/stdlib/js/src/generated",
    "libraries/stdlib/js/src/kotlin/collectionsExternal.kt",

    // Full version is defined in stdlib
    // This file is useful for smaller subset of runtime sources
    "libraries/stdlib/js/irRuntime/rangeExtensions.kt",

    // Mostly array-specific stuff
    "libraries/stdlib/js/src/kotlin/builtins.kt",

    // coroutines
    // TODO: merge coroutines_13 with JS BE coroutines
    "libraries/stdlib/js/src/kotlin/coroutines/intrinsics/IntrinsicsJs.kt",
    "libraries/stdlib/js/src/kotlin/coroutines/CoroutineImpl.kt",

    // Inlining of js fun doesn't update the variables inside
    "libraries/stdlib/js/src/kotlin/jsTypeOf.kt",
    "libraries/stdlib/js/src/kotlin/collections/utils.kt",

    // TODO: Remove stub
    "libraries/stdlib/js/src/kotlin/builtins.kt"
)

val reducedRuntimeSources = fullRuntimeSources - listOfKtFilesFrom(
    "libraries/stdlib/unsigned",
    "core/builtins/src/kotlin/reflect/KParameter.kt",
    "core/builtins/src/kotlin/reflect/KType.kt",
    "core/builtins/src/kotlin/reflect/KTypeParameter.kt",
    "core/builtins/src/kotlin/reflect/KVisibility.kt",
    "libraries/stdlib/common/src/generated/_Arrays.kt",
    "libraries/stdlib/common/src/generated/_Collections.kt",
    "libraries/stdlib/common/src/generated/_Maps.kt",
    "libraries/stdlib/common/src/generated/_Sequences.kt",
    "libraries/stdlib/common/src/generated/_Sets.kt",
    "libraries/stdlib/common/src/generated/_Strings.kt",
    "libraries/stdlib/common/src/generated/_UArrays.kt",
    "libraries/stdlib/common/src/generated/_URanges.kt",
    "libraries/stdlib/common/src/kotlin/SequencesH.kt",
    "libraries/stdlib/common/src/kotlin/TextH.kt",
    "libraries/stdlib/common/src/kotlin/collections/",
    "libraries/stdlib/common/src/kotlin/ioH.kt",
    "libraries/stdlib/js/irRuntime/collectionsHacks.kt",
    "libraries/stdlib/js/irRuntime/generated/",
    "libraries/stdlib/js/src/kotlin/char.kt",
    "libraries/stdlib/js/src/kotlin/collections.kt",
    "libraries/stdlib/js/src/kotlin/collections/",
    "libraries/stdlib/js/src/kotlin/console.kt",
    "libraries/stdlib/js/src/kotlin/coreDeprecated.kt",
    "libraries/stdlib/js/src/kotlin/date.kt",
    "libraries/stdlib/js/src/kotlin/debug.kt",
    "libraries/stdlib/js/src/kotlin/grouping.kt",
    "libraries/stdlib/js/src/kotlin/json.kt",
    "libraries/stdlib/js/src/kotlin/numberConversions.kt",
    "libraries/stdlib/js/src/kotlin/promise.kt",
    "libraries/stdlib/js/src/kotlin/regex.kt",
    "libraries/stdlib/js/src/kotlin/regexp.kt",
    "libraries/stdlib/js/src/kotlin/sequence.kt",
    "libraries/stdlib/js/src/kotlin/string.kt",
    "libraries/stdlib/js/src/kotlin/stringsCode.kt",
    "libraries/stdlib/js/src/kotlin/text.kt",
    "libraries/stdlib/src/kotlin/collections/",
    "libraries/stdlib/src/kotlin/experimental/bitwiseOperations.kt",
    "libraries/stdlib/src/kotlin/properties/Delegates.kt",
    "libraries/stdlib/src/kotlin/random/URandom.kt",
    "libraries/stdlib/src/kotlin/text/",
    "libraries/stdlib/src/kotlin/util/KotlinVersion.kt",
    "libraries/stdlib/src/kotlin/util/Tuples.kt",
    "libraries/stdlib/common/src/generated/_Comparisons.kt"
)

private fun listOfKtFilesFrom(vararg paths: String): List<String> {
    val currentDir = File(".")
    return paths.flatMap { path ->
        File(path)
            .walkTopDown()
            .filter { it.extension == "kt" }
            .map { it.relativeToOrSelf(currentDir).path }
            .asIterable()
    }.distinct()
}