/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the licenses/LICENSE.txt file.
 */

import org.jetbrains.benchmarksLauncher.*

actual class NumericalLauncher : Launcher() {
    actual override val baseBenchmarksSet = mutableMapOf(
            "BellardPi" to BenchmarkEntry(::jvmBellardPi)
    )

}

fun jvmBellardPi() {
    for (n in 1 .. 1000 step 9) {
        val result = pi_nth_digit(n)
        Blackhole.consume(result)
    }
}
