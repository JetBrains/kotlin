/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.example

enum class MyEnum {
    A, B
}

fun main() {
    val a = MyEnum.entries
    val b = AnnotationTarget.entries
    println("$a :: $b")
}
