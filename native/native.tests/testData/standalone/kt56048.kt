// TARGET_BACKEND: NATIVE
// EXIT_CODE: !0
// OUTPUT_REGEX: .*Converting Obj-C blocks with non-reference-typed return value to kotlin.Any is not supported \(v\).*kfun:#main.*

// MODULE: cinterop
// FILE: objclib.def
language = Objective-C
---
id getVoidBlockAsId() {
    void (^result)(void) = ^{};
    return result;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import objclib.*

fun main() {
    getVoidBlockAsId()
}
