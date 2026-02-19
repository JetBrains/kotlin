// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: mangling.def
---

// test mangling of special names

enum _Companion {Companion, Any};
enum _Companion companion = Companion;

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlinx.cinterop.*
import kotlin.test.*
import mangling.*

fun box(): String {
    companion = _Companion.`Companion$`
    assertEquals(_Companion.`Companion$`, companion)

    return "OK"
}

