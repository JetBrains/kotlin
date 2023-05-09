/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import com.intellij.util.containers.HashSetInterner


class Interner<T : Any> {
    private val pool = HashSetInterner<T>()

    fun intern(value: T): T = pool.intern(value)
}
