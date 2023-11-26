// OUTPUT_DATA_FILE: boxing10.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    val FALSE: Boolean? = false

    if (FALSE != null) {
        do {
            println("Ok")
        } while (FALSE)
    }
    return "OK"
}
