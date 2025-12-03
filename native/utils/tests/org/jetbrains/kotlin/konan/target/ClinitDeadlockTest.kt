/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.junit.jupiter.api.Test
import kotlin.concurrent.*


class ClinitDeadlockTest {

    @Volatile
    var ready = false

    @Test
    fun konanTargetInitializationDeadlock() {
        val size = 10
        val threadsHM = List(size) {
            thread {
                while (!ready) {}
                HostManager.host
            }
        }
        val threadsKT = List(size) {
            thread {
                while (!ready) {}
                KonanTarget.predefinedTargets
            }
        }
        ready = true
        (threadsHM + threadsKT).forEach { it.join() }
    }
}
