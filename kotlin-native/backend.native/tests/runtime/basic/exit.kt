/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.system.*

fun main(args: Array<String>) {
    exitProcess(42)
    @Suppress("UNREACHABLE_CODE")
    throw RuntimeException("Exit function call returned normally")
}