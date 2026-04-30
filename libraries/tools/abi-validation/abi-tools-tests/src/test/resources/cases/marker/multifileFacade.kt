/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:[JvmName("MCF") JvmMultifileClass]
package cases.marker

@HiddenMethod
public fun publicFunction(): Int = 42

@HiddenProperty
public var publicProperty: Int = 42

public class PublicClass {
    @HiddenField
    var f = 42

    @HiddenProperty
    var p = 42

    @HiddenMethod
    fun f(x: Int): Int = x + p - f
}
