/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.example

fun best(): Int {
    return 42
}

fun simpleBest(): Int {
    return 73
}

fun main(args: Array<String>) {
    println("ACCEPTED: ${args.drop(2).joinToString(";")}")
}