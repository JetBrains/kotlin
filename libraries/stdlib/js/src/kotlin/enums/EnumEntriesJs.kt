/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.enums

@SinceKotlin("1.9")
@ExperimentalStdlibApi
@PublishedApi
internal actual inline fun <reified T : Enum<T>> enumEntriesIntrinsic(): EnumEntries<T> {
    /*
     * Implementation note: this body will be replaced with `throw NotImplementedException()` the moment
     * all backends starts intrinsifying this call.
     */
    return enumEntries(enumValues<T>())
}