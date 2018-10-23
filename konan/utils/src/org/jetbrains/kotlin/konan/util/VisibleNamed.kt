/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.util

val <T : Enum<T>> T.visibleName get() = name.toLowerCase()

interface Named {
    val name: String
    val visibleName get() = name
}
