@file:Suppress("RemoveExplicitTypeArguments")

import org.jetbrains.kotlin.tooling.core.*
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
        assertNull(extras[extraKey<String>()])
        assertNull(extras[extraKey<String>("a")])
        assertNull(extras[extraKey<String>("b")])

        extras[extraKey()] = "22222"
        assertEquals("22222", extras[extraKey()])
        assertNull(extras[extraKey("a")])
        assertNull(extras[extraKey("b")])

        extras[extraKey("a")] = "value a"
        assertEquals("22222", extras[extraKey()])
        assertEquals("value a", extras[extraKey("a")])
        assertNull(extras[extraKey("b")])

        extras[extraKey("b")] = "value b"
        assertEquals("22222", extras[extraKey()])
        assertEquals("value a", extras[extraKey("a")])
        assertEquals("value b", extras[extraKey("b")])

        assertNull(extras[extraKey("c")])
    }

    @Test
    fun `test - ids`() {
        val stringKey = extraKey<String>()
        val stringKeyA = extraKey<String>("a")
        val intKey = extraKey<Int>()
        val intKeyA = extraKey<Int>("a")

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
        val stringKey = extraKey<String>()
        val stringKeyA = extraKey<String>("a")
        val intKey = extraKey<Int>()
        val intKeyA = extraKey<Int>("a")

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
        val stringKey = extraKey<String>()
        val stringKeyA = extraKey<String>("a")
        val intKey = extraKey<Int>()
        val intKeyA = extraKey<Int>("a")

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

        assertNotEquals(extras, extras + extraKey<Int>("b").withValue(2))
        assertNotEquals(mutableExtras, mutableExtras + extraKey<Int>("b").withValue(2))
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
        val key = extraKey<Int>()
        val extras = mutableExtrasOf()
        assertNull(extras.set(key, 1))
        assertEquals(1, extras[key])
        assertEquals(1, extras.set(key, 2))
        assertEquals(2, extras[key])
    }

    @Test
    fun `test - overwrite - immutable`() {
        val key = extraKey<Int>()
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

        val keyA = extraKey<Int>()
        val keyB = extraKey<Int>().withCapability(capability)

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

        val keyA = extraKey<Int>()
        val keyB = extraKey<Int>().withCapability(capability)

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
        assertEquals(extraKey<Int>(), extraKey<Int>())
        assertEquals(extraKey<List<String>>(), extraKey<List<String>>())
        assertEquals(extraKey<Int>("a"), extraKey<Int>("a"))
        assertNotEquals<Extras.Key<*>>(extraKey<Int>(), extraKey<String>())
        assertNotEquals<Extras.Key<*>>(extraKey<Int>("a"), extraKey<Int>())
        assertNotEquals<Extras.Key<*>>(extraKey<Int>("a"), extraKey<Int>("b"))

        val capabilityA = object : Extras.Key.Capability<Int> {}
        val capabilityB = object : Extras.Key.Capability<Int> {}

        assertNotEquals(extraKey<Int>().withCapability(capabilityA), extraKey<Int>())
        assertNotEquals(extraKey<Int>(), extraKey<Int>().withCapability(capabilityA))
        assertEquals(extraKey<Int>().withCapability(capabilityA), extraKey<Int>().withCapability(capabilityA))

        assertEquals(
            extraKey<Int>().withCapability(capabilityA),
            extraKey<Int>().withCapability(capabilityA).withCapability(capabilityA)
        )

        assertNotEquals(
            extraKey<Int>().withCapability(capabilityA),
            extraKey<Int>().withCapability(capabilityA).withCapability(capabilityB)
        )
    }

    @Test
    fun `test - add two extras`() {
        val capability = object : Extras.Key.Capability<Int> {}

        val keyA = extraKey<Int>("a")
        val keyB = extraKey<Int>("b")
        val keyC = extraKey<Int>("c")
        val keyD = extraKey<Int>()
        val keyE = extraKey<Int>() + capability

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
}
