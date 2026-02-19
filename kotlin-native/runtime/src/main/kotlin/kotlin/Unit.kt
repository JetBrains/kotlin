/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.native.internal.ExportTypeInfo

/**
 * The type with only one value: the `Unit` object.
 */
@ExportTypeInfo("theUnitTypeInfo")
public actual object Unit {
    override fun toString(): String = "kotlin.Unit"
}