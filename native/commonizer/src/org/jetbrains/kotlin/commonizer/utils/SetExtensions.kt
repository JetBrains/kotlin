/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

infix fun <T> Set<T>.isSubsetOf(other: Set<T>): Boolean {
    if (this === other) return true
    return other.containsAll(this)
}

infix fun <T> Set<T>.isProperSubsetOf(other: Set<T>): Boolean {
    if (this == other) return false
    return other.containsAll(this)
}
