/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.enums

@SinceKotlin("1.9")
@ExperimentalStdlibApi
@PublishedApi
// TODO: After the expect fun enumEntriesIntrinsic become non-inline function, the suppress and external keyword should be removed
@Suppress("INLINE_EXTERNAL_DECLARATION", "WRONG_JS_INTEROP_TYPE")
internal actual external inline fun <reified T : Enum<T>> enumEntriesIntrinsic(): EnumEntries<T>