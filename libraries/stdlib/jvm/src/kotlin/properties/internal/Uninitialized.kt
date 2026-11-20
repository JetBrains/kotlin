/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.properties.internal

@PublishedApi
@SinceKotlin("2.5")
internal actual object UNINITIALIZED : Serializable {
    private const val serialVersionUID = 0L

    private fun readResolve(): Any = UNINITIALIZED
}
