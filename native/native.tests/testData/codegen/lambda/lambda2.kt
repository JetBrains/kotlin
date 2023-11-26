// OUTPUT_DATA_FILE: lambda2.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    main(arrayOf("arg0"))

    return "OK"
}

fun main(args : Array<String>) {
    run {
        println(args[0])
    }
}

fun run(f: () -> Unit) {
    f()
}
