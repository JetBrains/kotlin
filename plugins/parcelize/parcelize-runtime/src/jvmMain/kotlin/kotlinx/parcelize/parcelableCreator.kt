/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")
package kotlinx.parcelize

import android.os.Parcelable

/**
 * Read the CREATOR field of the given [Parcelable] class. Calls to this function with
 * a concrete class will be optimized to a direct field access.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Parcelable> parcelableCreator(): Parcelable.Creator<T> =
    T::class.java.getDeclaredField("CREATOR").get(null) as? Parcelable.Creator<T>
        ?: throw IllegalArgumentException("Could not access CREATOR field in class ${T::class.simpleName}")
