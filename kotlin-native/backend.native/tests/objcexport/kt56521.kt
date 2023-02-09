/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

var initialized = 0

object Kt56521 {
    init {
        initialized = 1
    }
}

fun getKt56521(): Kt56521 {
    Kt56521
    return Kt56521
}
