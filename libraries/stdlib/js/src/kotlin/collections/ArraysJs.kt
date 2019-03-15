/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections


@UseExperimental(ExperimentalUnsignedTypes::class)
@SinceKotlin("1.3")
@kotlin.js.JsName("contentDeepHashCodeImpl")
internal fun <T> Array<out T>.contentDeepHashCodeImpl(): Int {
    var result = 1
    for (element in this) {
        val elementHash = when {
            element == null -> 0
            js("Kotlin").isArrayish(element) -> (element.unsafeCast<Array<*>>()).contentDeepHashCodeImpl()

            element is UByteArray   -> element.contentHashCode()
            element is UShortArray  -> element.contentHashCode()
            element is UIntArray    -> element.contentHashCode()
            element is ULongArray   -> element.contentHashCode()

            else                    -> element.hashCode()
        }

        result = 31 * result + elementHash
    }
    return result
}