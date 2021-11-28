/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


inline fun foo() {
    try {
        try {
            throw Exception("XXX")
        } catch (e: Throwable) {
            println("Gotcha1: ${e.message}")
            throw Exception("YYY")
        }
    } catch (e: Throwable) {
        println("Gotcha2: ${e.message}")
    }
}
