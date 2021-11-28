/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:Suppress("UNUSED")

package multiple

interface I1 {
    fun getFortyTwo(): Int
}

class I1Impl : I1 {
    override fun getFortyTwo(): Int = 42
}

fun getI1() = object : I1 {
    override fun getFortyTwo(): Int = 42
}

class C

fun getUnit(): Unit? = Unit

/*
// Disabled for now to avoid depending on platform libs.
fun getAnonymousObject() = object : platform.darwin.NSObject() {}
class NamedObject : platform.darwin.NSObject()
fun getNamedObject() = NamedObject()
 */