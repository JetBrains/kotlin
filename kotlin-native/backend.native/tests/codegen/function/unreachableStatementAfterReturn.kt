/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.function.unreachable_statement_after_return

fun test1(): Any? {
    return 1
    42
}

fun test2(): Int? {
    return 2
    42
}

fun test3(): Any {
    return 3
    42
}

fun test4(): Int {
    return 4
    42
}

fun main() {
    println(test1())
    println(test2())
    println(test3())
    println(test4())
}

