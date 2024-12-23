/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

internal object ListCompanionObject {
    /*operator*/ fun <T> of(vararg elements: T): List<T> = if (elements.size > 0) elements.asList() else emptyList()
    /*operator*/ fun <T> of(element: T): List<T> = java.util.Collections.singletonList(element)
    /*operator*/ fun <T> of(): List<T> = emptyList()
}
