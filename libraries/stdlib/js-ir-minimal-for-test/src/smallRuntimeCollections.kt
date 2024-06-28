/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

@Suppress("UNUSED_PARAMETER")
@SinceKotlin("1.4")
@library("arrayEquals")
public infix fun <T> Array<out T>?.contentEquals(other: Array<out T>?): Boolean {
    definedExternally
}