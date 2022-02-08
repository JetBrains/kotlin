/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:Suppress("UNUSED")

package multiple

interface I2 {
    fun getFortyTwo(): Int
}

fun getFortyTwoFrom(i2: I2): Int = i2.getFortyTwo()

fun getI2() = object : I2 {
    override fun getFortyTwo(): Int = 42
}

class C

fun isUnit(obj: Any?): Boolean = (obj === Unit)

/*
// Disabled for now to avoid depending on platform libs.
fun getAnonymousObject() = object : platform.darwin.NSObject() {}
class NamedObject : platform.darwin.NSObject()
fun getNamedObject() = NamedObject()
 */