/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.cinterop

import sun.misc.Unsafe

private val unsafe: Unsafe = with(Unsafe::class.java.getDeclaredField("theUnsafe")) {
    isAccessible = true
    return@with this.get(null) as Unsafe
}

@PublishedApi
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
internal inline fun <reified T> unsafeAllocateInstance(): T {
    // TODO KT-66632: get rid of this.
    return unsafe.allocateInstance(T::class.java) as T
}
