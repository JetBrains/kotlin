/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.project.modelx.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KProperty1

open class ToPrimitiveSerializer<T, P>(
    kind: PrimitiveKind,
    serialName: String,
    val getter: T.() -> P,
    val constructor: (P) -> T,
    val encode: Encoder.(P) -> Unit,
    val decode: Decoder.() -> P
) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(serialName, kind)
    override fun serialize(encoder: Encoder, value: T) = encoder.encode(getter.invoke(value))
    override fun deserialize(decoder: Decoder) = constructor(decoder.decode())
}

open class ToStringSerializer<T>(
    serialName: String,
    getter: T.() -> String,
    constructor: (String) -> T,
) : ToPrimitiveSerializer<T, String>(PrimitiveKind.STRING, serialName, getter, constructor, Encoder::encodeString, Decoder::decodeString)

open class ToIntSerializer<T>(
    serialName: String,
    getter: T.() -> Int,
    constructor: (Int) -> T,
) : ToPrimitiveSerializer<T, Int>(PrimitiveKind.INT, serialName, getter, constructor, Encoder::encodeInt, Decoder::decodeInt)
