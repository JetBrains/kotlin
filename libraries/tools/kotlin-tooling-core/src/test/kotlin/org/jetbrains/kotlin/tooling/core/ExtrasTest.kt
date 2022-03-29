@file:Suppress("RemoveExplicitTypeArguments")

package org.jetbrains.kotlin.tooling.core

import kotlin.test.*

class ExtrasTest {

    @Test
    fun `test - isEmpty`() {
        assertTrue(mutableExtrasOf().isEmpty())
        assertTrue(extrasOf().isEmpty())
        assertTrue(emptyExtras().isEmpty())

        assertFalse(mutableExtrasOf().isNotEmpty())
        assertFalse(extrasOf().isNotEmpty())
        assertFalse(emptyExtras().isNotEmpty())
    }

    @Test
    fun `test - add and get`() {
        val extras = mutableExtrasOf()
        assertNull(extras[extrasKeyOf<String>()])
        assertNull(extras[extrasKeyOf<String>("a")])
        assertNull(extras[extrasKeyOf<String>("b")])

        extras[extrasKeyOf()] = "22222"
        assertEquals("22222", extras[extrasKeyOf()])
        assertNull(extras[extrasKeyOf("a")])
        assertNull(extras[extrasKeyOf("b")])

        extras[extrasKeyOf("a")] = "value a"
        assertEquals("22222", extras[extrasKeyOf()])
        assertEquals("value a", extras[extrasKeyOf("a")])
        assertNull(extras[extrasKeyOf("b")])

        extras[extrasKeyOf("b")] = "value b"
        assertEquals("22222", extras[extrasKeyOf()])
        assertEquals("value a", extras[extrasKeyOf("a")])
        assertEquals("value b", extras[extrasKeyOf("b")])

        assertNull(extras[extrasKeyOf("c")])
    }

    @Test
    fun `test - ids`() {
        val stringKey = extrasKeyOf<String>()
        val stringKeyA = extrasKeyOf<String>("a")
        val intKey = extrasKeyOf<Int>()
        val intKeyA = extrasKeyOf<Int>("a")

        val ids = setOf(stringKey.id, stringKeyA.id, intKey.id, intKeyA.id)

        val extras = extrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        val mutableExtras = mutableExtrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        assertEquals(ids, extras.ids)
        assertEquals(ids, mutableExtras.ids)
    }

    @Test
    fun `test - keys`() {
        val stringKey = extrasKeyOf<String>()
        val stringKeyA = extrasKeyOf<String>("a")
        val intKey = extrasKeyOf<Int>()
        val intKeyA = extrasKeyOf<Int>("a")

        val keys = listOf(stringKey, stringKeyA, intKey, intKeyA)

        val extras = extrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        val mutableExtras = mutableExtrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        assertEquals(keys, extras.entries.map { it.key })
        assertEquals(keys, mutableExtras.entries.map { it.key })
    }

    @Test
    fun `test - equality`() {
        val stringKey = extrasKeyOf<String>()
        val stringKeyA = extrasKeyOf<String>("a")
        val intKey = extrasKeyOf<Int>()
        val intKeyA = extrasKeyOf<Int>("a")

        val extras = extrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        val mutableExtras = mutableExtrasOf(
            stringKey withValue "string",
            stringKeyA withValue "stringA",
            intKey withValue 1,
            intKeyA withValue 2
        )

        assertEquals(extras, mutableExtras)
        assertEquals(extras, mutableExtras.toExtras())
        assertEquals(extras.toExtras(), mutableExtras.toExtras())
        assertEquals(extras.toMutableExtras(), mutableExtras.toExtras())
        assertEquals(extras.toMutableExtras(), mutableExtras)
        assertEquals(mutableExtras, extras)

        assertNotEquals(extras, extras + extrasKeyOf<Int>("b").withValue(2))
        assertNotEquals(mutableExtras, mutableExtras + extrasKeyOf<Int>("b").withValue(2))
    }

    @Test
    fun `test - equality - empty`() {
        val extras0 = extrasOf()
        val extras1 = mutableExtrasOf()
        val extras2 = mutableExtrasOf()

        assertNotSame(extras0, extras1)
        assertNotSame(extras1, extras2)
        assertEquals(extras0, extras1)
        assertEquals(extras1, extras2)
        assertEquals(extras2, extras1)
        assertEquals(emptyExtras(), extras0)
        assertEquals(emptyExtras(), extras1)
        assertEquals(extras0, emptyExtras())
        assertEquals(extras1, emptyExtras())
        assertEquals(emptyExtras(), extras2)
        assertEquals(extras2, emptyExtras())

        assertEquals(extras0.hashCode(), extras1.hashCode())
        assertEquals(extras1.hashCode(), extras2.hashCode())
        assertEquals(extras1.hashCode(), emptyExtras().hashCode())
    }

    @Test
    fun `test - overwrite - mutable`() {
        val key = extrasKeyOf<Int>()
        val extras = mutableExtrasOf()
        assertNull(extras.set(key, 1))
        assertEquals(1, extras[key])
        assertEquals(1, extras.set(key, 2))
        assertEquals(2, extras[key])
    }

    @Test
    fun `test - overwrite - immutable`() {
        val key = extrasKeyOf<Int>()
        val extras0 = extrasOf()
        val extras1 = extras0 + (key withValue 1)
        assertNull(extras0[key])
        assertEquals(1, extras1[key])

        val extras2 = extras1 + (key withValue 2)
        assertNull(extras0[key])
        assertEquals(1, extras1[key])
        assertEquals(2, extras2[key])
    }

    @Test
    fun `test - overwrite - mutable - withCapability`() {
        val capability = object : Extras.Key.Capability<Int> {}

        val keyA = extrasKeyOf<Int>()
        val keyB = extrasKeyOf<Int>() + capability

        val extras = mutableExtrasOf()
        extras[keyA] = 0
        assertEquals(0, extras.set(keyB, 1))
        assertEquals(1, extras[keyB])
        assertEquals(1, extras[keyA])

        assertEquals(setOf(keyB.id), extras.ids)
    }

    @Test
    fun `test - overwrite - immutable - withCapability`() {
        val capability = object : Extras.Key.Capability<Int> {}

        val keyA = extrasKeyOf<Int>()
        val keyB = extrasKeyOf<Int>() + capability

        val extras = extrasOf()
        val extras1 = extras + (keyA withValue 1)
        val extras2 = extras1 + (keyB withValue 2)

        assertEquals(1, extras1[keyA])
        assertEquals(1, extras1[keyB])
        assertEquals(2, extras2[keyA])
        assertEquals(2, extras2[keyB])
    }

    @Test
    fun `test - key equality`() {
        assertEquals(extrasKeyOf<Int>(), extrasKeyOf<Int>())
        assertEquals(extrasKeyOf<List<String>>(), extrasKeyOf<List<String>>())
        assertEquals(extrasKeyOf<Int>("a"), extrasKeyOf<Int>("a"))
        assertNotEquals<Extras.Key<*>>(extrasKeyOf<Int>(), extrasKeyOf<String>())
        assertNotEquals<Extras.Key<*>>(extrasKeyOf<Int>("a"), extrasKeyOf<Int>())
        assertNotEquals<Extras.Key<*>>(extrasKeyOf<Int>("a"), extrasKeyOf<Int>("b"))

        val capabilityA = object : Extras.Key.Capability<Int> {}
        val capabilityB = object : Extras.Key.Capability<Int> {}

        assertNotEquals(extrasKeyOf<Int>() + capabilityA, extrasKeyOf<Int>())
        assertNotEquals(extrasKeyOf<Int>(), extrasKeyOf<Int>() + capabilityA)
        assertEquals(extrasKeyOf<Int>() + capabilityA, extrasKeyOf<Int>() + capabilityA)

        assertEquals(
            extrasKeyOf<Int>() + capabilityA,
            extrasKeyOf<Int>() + capabilityA + capabilityA
        )

        assertNotEquals(
            extrasKeyOf<Int>() + capabilityA,
            extrasKeyOf<Int>() + capabilityA + capabilityB
        )
    }

    @Test
    fun `test - add two extras`() {
        val capability = object : Extras.Key.Capability<Int> {}

        val keyA = extrasKeyOf<Int>("a")
        val keyB = extrasKeyOf<Int>("b")
        val keyC = extrasKeyOf<Int>("c")
        val keyD = extrasKeyOf<Int>()
        val keyE = extrasKeyOf<Int>() + capability

        val extras1 = extrasOf(
            keyA withValue 0,
            keyB withValue 1,
            keyC withValue 2,
            keyD withValue 3
        )

        val extras2 = extrasOf(
            keyC withValue 4,
            keyE withValue 5
        )

        val combinedExtras = extras1 + extras2

        assertEquals(
            extrasOf(
                keyA withValue 0,
                keyB withValue 1,
                keyC withValue 4,
                keyE withValue 5
            ),
            combinedExtras
        )
    }

    @Test
    fun `test - mutable extras - remove`() {
        val extras = mutableExtrasOf(
            extrasKeyOf<String>() withValue "2",
            extrasKeyOf<String>("other") withValue "4",
            extrasKeyOf<Int>() withValue 1,
            extrasKeyOf<Int>("cash") withValue 1
        )

        extras.remove(extrasKeyOf<Int>("sunny"))
        assertEquals(1, extras[extrasKeyOf<Int>("cash")])

        assertEquals(1, extras.remove(extrasKeyOf<Int>("cash")))
        assertNull(extras[extrasKeyOf<Int>("cash")])

        assertEquals(1, extras.remove(extrasKeyOf<Int>()))
        assertNull(extras[extrasKeyOf<Int>()])

        assertEquals(
            extrasKeyOf<String>("other") withValue "4",
            extras.remove(extrasIdOf<String>("other"))
        )

        assertNull(extras[extrasKeyOf<String>("other")])

        assertEquals(
            extrasOf(extrasKeyOf<String>() withValue "2"),
            extras.toExtras()
        )
    }

    @Test
    fun `test - mutable extras - clear`() {
        val extras = mutableExtrasOf(
            extrasKeyOf<String>() withValue "2",
            extrasKeyOf<String>("other") withValue "4",
            extrasKeyOf<Int>() withValue 1,
            extrasKeyOf<Int>("cash") withValue 1
        )

        assertFalse(extras.isEmpty(), "Expected non-empty extras")
        extras.clear()
        assertTrue(extras.isEmpty(), "Expected extras to be empty")

        assertEquals(emptyExtras(), extras)
        assertEquals(emptyExtras(), extras.toExtras())
    }

    @Test
    fun `test mutable extras - putAll`() {
        val extras = mutableExtrasOf(
            extrasKeyOf<String>() withValue "1",
            extrasKeyOf<String>("overwrite") withValue "2"
        )

        extras.putAll(
            extrasOf(
                extrasKeyOf<String>("overwrite") withValue "3",
                extrasKeyOf<Int>() withValue 1
            )
        )

        assertEquals(
            extrasOf(
                extrasKeyOf<String>() withValue "1",
                extrasKeyOf<String>("overwrite") withValue "3",
                extrasKeyOf<Int>() withValue 1
            ),
            extras
        )
    }
}
