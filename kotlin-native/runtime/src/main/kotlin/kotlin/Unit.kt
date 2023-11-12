/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.native.internal.ExportTypeInfo

/**
 * The type with only one value: the `Unit` object.
 */
@ExportTypeInfo("theUnitTypeInfo")
public object Unit {
    override fun toString(): String = "kotlin.Unit"
}