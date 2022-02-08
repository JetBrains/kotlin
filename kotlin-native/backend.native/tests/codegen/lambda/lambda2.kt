/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lambda.lambda2

import kotlin.test.*

@Test fun runTest() {
    main(arrayOf("arg0"))
}

fun main(args : Array<String>) {
    run {
        println(args[0])
    }
}

fun run(f: () -> Unit) {
    f()
}