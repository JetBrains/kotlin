/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.tree.JKElement

inline fun <reified T : JKElement> JKElement.parentOfType(): T? {
    return generateSequence(parent) { it.parent }.filterIsInstance<T>().firstOrNull()
}