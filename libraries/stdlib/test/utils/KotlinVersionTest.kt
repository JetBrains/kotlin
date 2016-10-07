package test.utils

import org.junit.Test
import kotlin.test.*


class KotlinVersionTest {

    @Test fun currentVersion() {
        assertTrue(KotlinVersion.CURRENT.isAtLeast(1, 1))
        assertTrue(KotlinVersion.CURRENT.isAtLeast(1, 1, 0))
        assertTrue(KotlinVersion.CURRENT >= KotlinVersion(1, 1))
        assertTrue(KotlinVersion(1, 1) <= KotlinVersion.CURRENT)

        val anotherCurrent = KotlinVersion.CURRENT.run { KotlinVersion(major, minor, patch) }
        assertEquals(KotlinVersion.CURRENT, anotherCurrent)
        assertEquals(KotlinVersion.CURRENT.hashCode(), anotherCurrent.hashCode())
        assertEquals(0, KotlinVersion.CURRENT.compareTo(anotherCurrent))
    }

    @Test fun componentValidation() {
        for (component in listOf(Int.MIN_VALUE, -1, 0, KotlinVersion.MAX_COMPONENT_VALUE, KotlinVersion.MAX_COMPONENT_VALUE + 1, Int.MAX_VALUE)) {
            for (place in 0..2) {
                val (major, minor, patch) = IntArray(3) { index -> if (index == place) component else 0 }
                if (component in 0..KotlinVersion.MAX_COMPONENT_VALUE) {
                    KotlinVersion(major, minor, patch)
                }
                else {
                    assertFailsWith<IllegalArgumentException>("Expected $major.$minor.$patch to be invalid version") {
                        KotlinVersion(major, minor, patch)
                    }
                }
            }
        }
    }
}

