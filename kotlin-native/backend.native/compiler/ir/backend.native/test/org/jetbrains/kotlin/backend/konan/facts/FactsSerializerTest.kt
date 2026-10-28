/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.facts

import kotlin.test.*

class FactsSerializerTest {
    private val b = FactKey(FactKind.SIGNATURE, SymbolKey(0xF00D_0000_0000_0001UL)) { "/b|b(kotlin.Int){}[0]" }
    private val base = FactKey(FactKind.BASE_CLASS_LAYOUT, SymbolKey(42UL)) { "/Base|null[0]" }

    private val defs: DefsAssociation = mapOf(
            "/src/B.kt" to mapOf(b to ULong.MAX_VALUE),
            "/src/Base.kt" to mapOf(base to 7UL),
    )
    private val uses: UsesAssociation = mapOf(
            "/src/A.kt" to mapOf(b to ULong.MAX_VALUE),
            "/src/Derived.kt" to mapOf(base to 7UL),
    )

    @Test
    fun binaryRoundTrip() {
        val [actualDefs, actualUses] = BinaryFactsDeserializer.deserialize(BinaryFactsSerializer.serialize(defs, uses))
        assertEquals(defs, actualDefs)
        assertEquals(uses, actualUses)
    }

    @Test
    fun debugRoundTripKeepsRepresentation() {
        val [actualDefs, actualUses] = DebugFactsDeserializer.deserialize(DebugFactsSerializer.serialize(defs, uses))
        assertEquals(defs, actualDefs)
        assertEquals(uses, actualUses)
        assertEquals(b.representation, actualDefs.getValue("/src/B.kt").keys.single().representation)
    }

    @Test
    fun debugIsCsvWithHeader() {
        val lines = DebugFactsSerializer.serialize(defs, uses).toString(Charsets.UTF_8).lines()
        assertEquals("relation,path,kind,symbolKey,hash,representation", lines.first())
        assertEquals("defines,/src/B.kt,SIGNATURE,f00d000000000001,ffffffffffffffff,/b|b(kotlin.Int){}[0]", lines[1])
    }

    @Test
    fun debugQuotesSeparators() {
        val tricky = FactKey(FactKind.SIGNATURE, SymbolKey(1UL)) { "/m|m(kotlin.collections.Map<kotlin.String,kotlin.Int>){}[0] \"q\"" }
        val trickyDefs: DefsAssociation = mapOf("/src/a,b.kt" to mapOf(tricky to 2UL))
        val text = DebugFactsSerializer.serialize(trickyDefs, emptyMap()).toString(Charsets.UTF_8)
        assertTrue("\"/src/a,b.kt\"" in text, text)
        assertTrue("\"\"q\"\"" in text, text)
        val [actualDefs, actualUses] = DebugFactsDeserializer.deserialize(text.toByteArray(Charsets.UTF_8))
        assertEquals(trickyDefs, actualDefs)
        assertEquals(tricky.representation, actualDefs.getValue("/src/a,b.kt").keys.single().representation)
        assertTrue(actualUses.isEmpty())
    }

    @Test
    fun emptyRoundTrip() {
        val [actualDefs, actualUses] = BinaryFactsDeserializer.deserialize(BinaryFactsSerializer.serialize(emptyMap(), emptyMap()))
        assertTrue(actualDefs.isEmpty())
        assertTrue(actualUses.isEmpty())
    }

    @Test
    fun binaryRejectsOtherFormats() {
        assertFailsWith<IllegalArgumentException> {
            BinaryFactsDeserializer.deserialize(DebugFactsSerializer.serialize(defs, uses))
        }
    }
}
