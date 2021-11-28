/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


inline fun foo() {
    do {
        var x: Int = 999
        println(x)
    } while (x != 999)
}
