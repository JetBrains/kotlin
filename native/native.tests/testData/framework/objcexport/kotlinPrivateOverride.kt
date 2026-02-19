/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlinPrivateOverride

import kotlin.test.*

// The intention is to test "virtual adapters" machinery of ObjCExport.
// This machinery handles the case when exported Kotlin type is subclassed
// by a non-exported (e.g. private) Kotlin type, and the instance of the latter is passed to Obj-C.

interface KotlinPrivateOverrideI1 {
    fun i123AbstractMethod(): Int
    fun i1OpenMethod(): Int = -1
}

interface KotlinPrivateOverrideI2 {
    fun i123AbstractMethod(): Int
    fun i234AbstractMethod(): Int
    fun i2AbstractMethod(): Int
}

private interface KotlinPrivateOverridePI1 {
    fun pi1AbstractMethod(): Int
}

abstract class KotlinPrivateOverrideA1 : KotlinPrivateOverrideI1, KotlinPrivateOverrideI2, KotlinPrivateOverridePI1 {
    abstract fun a1AbstractMethod(): Int
    open fun a1OpenMethod(): Int = -2
}

interface KotlinPrivateOverrideI3 {
    fun i123AbstractMethod(): Int
    fun i234AbstractMethod(): Int
    fun i3AbstractMethod(): Int
}

private interface KotlinPrivateOverridePI2 {
    fun pi2AbstractMethod(): Int
}

private open class KotlinPrivateOverrideP1 : KotlinPrivateOverrideA1(), KotlinPrivateOverrideI3, KotlinPrivateOverridePI2 {
    override fun i123AbstractMethod(): Int = 1
    override fun i1OpenMethod(): Int = 2
    override fun i234AbstractMethod(): Int = 3
    override fun i2AbstractMethod(): Int = 4
    override fun pi1AbstractMethod(): Int = 5
    override fun a1AbstractMethod(): Int = 6
    override fun a1OpenMethod(): Int = 7
    override fun i3AbstractMethod(): Int = 8
    override fun pi2AbstractMethod(): Int = 9
}

fun createP1(): Any = KotlinPrivateOverrideP1()

interface KotlinPrivateOverrideI4 {
    fun i234AbstractMethod(): Int
    fun i4AbstractMethod(): Int
}

private interface KotlinPrivateOverridePI3 {
    fun pi3AbstractMethod(): Int
}

private class KotlinPrivateOverrideP12 : KotlinPrivateOverrideP1(), KotlinPrivateOverrideI4, KotlinPrivateOverridePI3 {
    override fun i123AbstractMethod(): Int = 11
    override fun i1OpenMethod(): Int = 12
    override fun i234AbstractMethod(): Int = 13
    override fun i2AbstractMethod(): Int = 14
    override fun pi1AbstractMethod(): Int = 15
    override fun a1AbstractMethod(): Int = 16
    override fun a1OpenMethod(): Int = 17
    override fun i3AbstractMethod(): Int = 18
    override fun pi2AbstractMethod(): Int = 19
    override fun i4AbstractMethod(): Int = 20
    override fun pi3AbstractMethod(): Int = 21
}

fun createP12(): Any = KotlinPrivateOverrideP12()
