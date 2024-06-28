// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: kt66169.def
language = C
---

typedef struct {
    int x;
    int y;
} S;

S createS() {
    S s = {1, 2};
    return s;
}

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kt66169.*
import kotlinx.cinterop.*
import kotlin.test.*

fun box(): String {
    val s = createS()

    val sX: Int
    val sY: Int

    s.useContents {
        sX = x
        sY = y
    }

    assertEquals(1, sX)
    assertEquals(2, sY)

    return "OK"
}
