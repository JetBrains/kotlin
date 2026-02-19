/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package examples.classes

public actual class Exp {
    constructor(a: Int, b: Int) {

    }

    actual fun a(): Int = 1
    actual fun b(): Int = 2
    actual fun c(): Int = 3

    actual var v1: Int = 1
    actual var v2: Int = 2
    actual var v3: Int = 3

    actual val vi1: Int = 4
    actual val vi2: Int = 5
    actual val vi3: Int = 6
}

val androidAndLinuxVal: Int = 0

class AndroidAndLinuxClass
