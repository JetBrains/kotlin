/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    main(emptyArray())

    return "OK"
}

fun main(args: Array<String>) {
    val b = args.size < 1
    val x = if (b) Any() else throw Error()
}