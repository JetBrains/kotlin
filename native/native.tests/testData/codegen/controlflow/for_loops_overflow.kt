// OUTPUT_DATA_FILE: for_loops_overflow.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    for (i in Int.MAX_VALUE - 1 .. Int.MAX_VALUE) { print(i); print(' ') }; println()
    for (i in Int.MAX_VALUE - 1 until Int.MAX_VALUE) { print(i); print(' ') }; println()
    for (i in Int.MIN_VALUE + 1 downTo Int.MIN_VALUE) { print(i); print(' ') }; println()

    // Empty loops
    for (i in Byte.MIN_VALUE  until Byte.MIN_VALUE)  { print(i); print(' ') }
    for (i in Short.MIN_VALUE until Short.MIN_VALUE) { print(i); print(' ') }
    for (i in Int.MIN_VALUE   until Int.MIN_VALUE)   { print(i); print(' ') }
    for (i in Long.MIN_VALUE  until Long.MIN_VALUE)  { print(i); print(' ') }
    for (i in 0.toChar()      until 0.toChar())      { print(i); print(' ') }

    for (i in 0 until Byte.MIN_VALUE)  { print(i); print(' ') }
    for (i in 0 until Short.MIN_VALUE) { print(i); print(' ') }
    for (i in 0 until Int.MIN_VALUE)   { print(i); print(' ') }
    for (i in 0 until Long.MIN_VALUE)  { print(i); print(' ') }
    for (i in 'a' until 0.toChar())    { print(i); print(' ') }


    val M = Int.MAX_VALUE / 2
    for (i in M + 4..M + 10 step M)  { print(i); print(' ') }; println()

    return "OK"
}
