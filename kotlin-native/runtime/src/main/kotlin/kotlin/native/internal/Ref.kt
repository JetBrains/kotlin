/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

/**
 * This class is used to allocate closure-captured variables in the heap.
 */
@PublishedApi
internal class Ref<T> {
    var element: T = undefined()
}