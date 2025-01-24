/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@file:kotlin.native.internal.objc.BindClassToObjCName(Base::class, "Base")
@file:kotlin.native.internal.objc.BindClassToObjCName(Derived::class, "Derived")

import cinterop.*
import kotlin.test.*

open class Base
open class Derived : Base()

fun main() {
    val derived = Derived()
    val externalRCRef = kotlin.native.internal.ref.createRetainedExternalRCRef(derived).toLong().toULong()
    assertTrue(test(externalRCRef))
}
