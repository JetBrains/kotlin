/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

fun box(): String {
    val unit = println("First")
    if (unit.toString() != "kotlin.Unit") return "FAIL 1: $unit"
    return "OK"
}