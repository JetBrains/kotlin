// FREE_COMPILER_ARGS: -Xbinary=latin1Strings=true
// MODULE: cinterop
// FILE: cinterop.def
---
static int checksum(const short* str, int length) {
    int sum = 0;
    for (int i = 0; i < length; ++i) {
        sum += str[i];
    }
    return sum;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cinterop.*
import kotlinx.cinterop.*

fun ktChecksum(str: String, offset: Int, length: Int): Int {
    var sum = 0
    (offset until offset + length).forEach {
        sum += str[it].code
    }
    return sum
}

fun box(): String {
    val str = "This is " + "an ASCII" + " String"
    val offset = 3
    val length = 10
    val expected = ktChecksum(str, offset, length)
    val actual = str.usePinned {
        // Even though `str` is Latin1, `it` here is UTF-16.
        checksum(it.addressOf(offset).reinterpret(), length)
    }
    if (expected == actual) {
        return "OK"
    } else {
        return "FAIL: expected=$expected actual=$actual"
    }
}
