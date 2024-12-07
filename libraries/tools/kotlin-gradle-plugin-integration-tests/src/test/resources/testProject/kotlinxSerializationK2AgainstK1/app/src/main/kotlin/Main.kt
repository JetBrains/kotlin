/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package foo

import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
class Derived(val d: Long = 1000000000000) : Base(2, "world", listOf("b", "c")) {
    override fun equals(other: Any?): Boolean {
        if (other !is Derived) return false
        return a == other.a && b == other.b && c == other.c && d == other.d
    }

    override fun toString(): String {
        return "a: $a, b: $b, c: $c, d: $d"
    }
}

fun processAbstractBase(abstractBase: AbstractBase) {
    println(abstractBase.x)
    println(abstractBase.y)
    println(abstractBase.nonSerializableProp)
}

fun main() {
    val expected = Derived(12)
    val result = Json.encodeToString(Derived.serializer(), expected)
    if (result != """{"c":2,"b":"world","a":["b","c"],"d":12}""") {
        throw IllegalStateException("Error: $result")
    }
    val actual = Json.decodeFromString(Derived.serializer(), result)
    if (expected != actual) {
        throw IllegalStateException("expected: $expected\nactual: $actual")
    }
}
