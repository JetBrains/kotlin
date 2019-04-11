/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

fun <T> List<T>.replace(element: T, replacer: T): List<T> {
    val mutableList = toMutableList()
    val index = indexOf(element)
    mutableList[index] = replacer
    return mutableList
}