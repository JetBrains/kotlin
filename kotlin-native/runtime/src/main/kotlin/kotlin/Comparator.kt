/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

public actual fun interface Comparator<T> {
    public actual fun compare(a: T, b: T): Int
}
