/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package examples.classes

public expect class Exp {
    fun a(): Int
    fun b(): Int
    fun c(): Int

    var v1: Int
    var v2: Int
    var v3: Int

    val vi1: Int
    val vi2: Int
    val vi3: Int
}
