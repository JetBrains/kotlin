/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.example.serialization_app

import com.example.serialization_lib.*
import kotlinx.serialization.json.*
import kotlinx.serialization.*

@Serializable
class EnumUsage(val access: Access)

fun main() {
    val s = Json.encodeToString(EnumUsage.serializer(), EnumUsage(Access.ReadWrite))
    if (s != """{"access":"rw"}""") throw AssertionError("Unexpected serialized form: $s")
    val usage = Json.decodeFromString<EnumUsage>(s)
    if (usage.access != Access.ReadWrite) throw AssertionError("Unexpected deserialized result: ${usage.access}")
}
