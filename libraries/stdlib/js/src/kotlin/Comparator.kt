/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:JsFileName("ComparatorJs")
package kotlin


public actual fun interface Comparator<T> {
    @JsName("compare")
    public actual fun compare(a: T, b: T): Int
}
