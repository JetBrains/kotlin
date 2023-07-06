/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.enums

import kotlin.native.internal.*

@SinceKotlin("1.9")
@ExperimentalStdlibApi
@PublishedApi
@TypedIntrinsic(IntrinsicType.ENUM_ENTRIES)
internal actual external inline fun <reified T : Enum<T>> enumEntriesIntrinsic(): EnumEntries<T>