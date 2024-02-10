/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

inline fun inner2() : Nothing {
    throw Exception()
}

fun inner1() : Nothing {
    inner2()
}

fun getStackTrace() : List<String> {
    try {
       inner1()
    }  catch (e: Exception) {
        return e.getStackTrace().toList()
    }
    return emptyList()
}