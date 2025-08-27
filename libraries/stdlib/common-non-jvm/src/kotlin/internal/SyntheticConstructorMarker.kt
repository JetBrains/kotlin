/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

/**
 * Marker object used by the Kotlin compiler to disambiguate synthetic constructors.
 *
 * In some phases the compiler may generate additional constructors that are not
 * meant to be called directly from user code. Such synthetic constructors may include
 * an extra trailing parameter of this marker type.
 */
@PublishedApi
@UsedFromCompilerGeneratedCode
@SinceKotlin("2.3")
internal object SyntheticConstructorMarker