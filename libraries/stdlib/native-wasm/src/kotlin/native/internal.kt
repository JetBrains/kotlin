/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal

internal expect interface KonanSet<out E> : Set<E> {
    fun getElement(element: @UnsafeVariance E): E?
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal expect annotation class CanBePrecreated()