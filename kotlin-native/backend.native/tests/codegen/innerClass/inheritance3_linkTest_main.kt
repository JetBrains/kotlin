/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

fun main() {
    val o = Outer().Middle().Inner2()
    println(o.getOuter() != Outer())
    println(o.getMiddle() != Outer().Middle())
}