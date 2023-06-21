/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test.enums

import org.junit.Test
import test.collections.behaviors.listBehavior
import test.collections.compare
import test.io.deserializeFromByteArray
import test.io.serializeAndDeserialize
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries
import kotlin.test.assertEquals

@Suppress("UNUSED_EXPRESSION", "EnumValuesSoftDeprecate") // <- Used deliberately as a sanity check for tests
class EnumEntriesJvmTest {
    enum class EmptyEnum

    enum class NonEmptyEnum {
        A, B, C
    }

    @Test
    fun testEmptyEnumSerialization() {
        val entries = serializeAndDeserialize(enumEntries(EmptyEnum::values))
        compare(EmptyEnum.values().toList(), entries) { listBehavior() }
    }

    @Test
    fun testNonEmptyEnumSerialization() {
        val entries = serializeAndDeserialize(enumEntries(NonEmptyEnum::values))
        compare(NonEmptyEnum.values().toList(), entries) { listBehavior() }
    }

    @Test
    fun testLambdaIsNotSerialized() {
        val nonSerializable = object {}  // Deliberately non-serializable
        val entries = enumEntries {
            nonSerializable // Capture it
            EmptyEnum.values()
        }

        val newEntries = serializeAndDeserialize(entries)
        assertEquals(entries, newEntries)
    }

    // Declarations for enum evolution test

    /*
     * This is the serialized contents of
     * ```
     * enum class Evolved {}
     * ```
     * without ANY entries.
     */
    private val bytes =
        ("-84,-19,0,5,115,114,0,42,107,111,116,108,105,110,46,101,110,117,109,115,46,69,110,117,109,69,110,116,114," +
                "105,101,115,83,101,114,105,97,108,105,122,97,116,105,111,110,80,114,111,120,121,0,0,0,0,0,0,0,0,2," +
                "0,1,76,0,1,99,116,0,17,76,106,97,118,97,47,108,97,110,103,47,67,108,97,115,115,59,120,112,118,114,0," +
                "37,116,101,115,116,46,101,110,117,109,115,46,69,110,117,109,69,110,116,114,105,101,115,74,118,109,84," +
                "101,115,116,36,69,118,111,108,118,101,100,0,0,0,0,0,0,0,0,18,0,0,120,114,0,14,106,97,118,97,46,108,97," +
                "110,103,46,69,110,117,109,0,0,0,0,0,0,0,0,18,0,0,120,112")
            .split(",").map { it.toInt().toByte() }.toByteArray()

    // Emulate enum evolution
    enum class Evolved {
        E1, E2, E3
    }

    @Test
    fun testEnumEvolution() {
        // Test checks that if the enum has new members after being serialized, they are all still deserialized properly
        val list = deserializeFromByteArray<EnumEntries<Evolved>>(bytes)
        assertEquals(enumEntries(Evolved::values), list)
    }
}
