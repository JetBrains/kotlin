/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal

// Only for compatibility with shared K/N stdlib code

internal actual interface KonanSet<out E> : Set<E> {
    actual fun getElement(element: @UnsafeVariance E): E?
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal actual annotation class CanBePrecreated
