// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: cacheMode=STATIC_ONLY_DIST
// DISABLE_NATIVE: cacheMode=STATIC_EVERYWHERE
// DISABLE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:*
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:*

// // https://youtrack.jetbrains.com/issue/KT-69731
// DISABLE_NATIVE: gcType=CMS
// FREE_COMPILER_ARGS: -Xbinary=escapeAnalysisPropagateExiledToHeapObjects=false

// Enable runtime assertions:
// ASSERTIONS_MODE: always-enable

@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.internal.isStack
import kotlin.native.runtime.GC

class Node {
    var next: Node? = null
}

fun box(): String {
    val head = Node()
    if (!head.isStack()) return "FAIL 1"

    val tail = Node()
    if (!tail.isStack()) return "FAIL 2"

    repeat(1) {
        val middle = Node()
        if (middle.isStack()) return "FAIL 3"

        head.next = middle
        middle.next = tail
    }

    // A runtime assertion should fail in GC if we have a reference
    // from a heap non-local object (`middle`) to a stack object (`tail`):
    GC.collect()

    return "OK"
}
