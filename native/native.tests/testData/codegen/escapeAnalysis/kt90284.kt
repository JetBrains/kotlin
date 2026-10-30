// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: cacheMode=STATIC_ONLY_DIST
// DISABLE_NATIVE: cacheMode=STATIC_EVERYWHERE
// DISABLE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:*
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:*

// Compressed references require a 64-bit non-MinGW target, CMS GC and the custom allocator:
// DISABLE_NATIVE: targetFamily=MINGW
// DISABLE_NATIVE: targetArchitecture=ARM32
// DISABLE_NATIVE: targetArchitecture=X86
// DISABLE_NATIVE: target=watchos_arm64
// DISABLE_NATIVE: gcType=NOOP
// DISABLE_NATIVE: gcType=STWMS
// DISABLE_NATIVE: gcType=PMCS
// DISABLE_NATIVE: alloc=STD
// FREE_COMPILER_ARGS: -Xbinary=compressedRefs=true

import kotlin.native.internal.isStack

class Node {
    var next: Node? = null
}

var condition = true

fun box(): String {
    val head = Node()
    if (!head.isStack()) return "FAIL 1"

    val tail = Node()
    if (tail.isStack()) return "FAIL 2"

    head.next = tail

    val otherHead = if (condition) Node() else Node()
    if (!otherHead.isStack()) return "FAIL 3"

    val otherTail = Node()
    if (otherTail.isStack()) return "FAIL 4"

    otherHead.next = otherTail

    return "OK"
}
