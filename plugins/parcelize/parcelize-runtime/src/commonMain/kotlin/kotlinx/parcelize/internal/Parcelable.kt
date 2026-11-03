/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.parcelize.internal

/**
 * Multiplatform supertype that the Parcelize plugin adds to [kotlinx.parcelize.Parcelize] declarations when `Parcelable` is missing and
 * `implicitlyAddParcelableSupertype` is enabled.
 *
 * Only the Parcelize plugin may use this type. Do not refer to it in your code.
 */
expect interface Parcelable
