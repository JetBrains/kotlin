// OUTPUT_DATA_FILE: loop.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

enum class Zzz {
    Z {
        init {
            println(Z.name)
        }
    }
}

fun box(): String {
    println(Zzz.Z)

    return "OK"
}
