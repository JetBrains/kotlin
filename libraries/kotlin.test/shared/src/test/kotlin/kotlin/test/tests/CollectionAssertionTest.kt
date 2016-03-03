@file:Suppress("DEPRECATION")
package kotlin.test.tests

import org.junit.*
import java.util.*
import kotlin.test.*

class CollectionAssertionTest {
    @Test
    fun testList() {
        assert(listOf(1, 2, 3)) {
            sizeShouldBe(3)
            elementAtShouldBe(0, 1)
            elementAtShouldComply(0) { it > 0 }
            lastElementShouldBe(3)
            containsAll(1, 2)

            shouldBe(listOf(1, 2, 3))
        }
    }

    @Test
    fun testSet() {
        assert(setOf(1, 2, 3)) {
            sizeShouldBe(3)
            elementAtShouldBe(0, 1)
            elementAtShouldComply(0) { it > 0 }
            lastElementShouldBe(3)
            containsAll(1, 2)

            shouldBeSet(setOf(1, 2, 3))
        }
    }

    @Test(expected = AssertionError::class)
    fun testSizeShouldBeFails() {
        assert(listOf(1, 2, 3)) {
            sizeShouldBe(1)
        }
    }

    @Test(expected = AssertionError::class)
    fun testElementAtShouldBeFail() {
        assert(listOf(1, 2, 3)) {
            elementAtShouldBe(0, 0)
        }
    }

    @Test(expected = AssertionError::class)
    fun testElementAtShouldComplyFail() {
        assert(listOf(1, 2, 3)) {
            elementAtShouldComply(0) { it < 0 }
        }
    }

    @Test(expected = AssertionError::class)
    fun testLastElementFail() {
        assert(listOf(1, 2, 3)) {
            lastElementShouldBe(0)
        }
    }

    @Test(expected = NoSuchElementException::class)
    fun testLastElementOnEmptyFail() {
        assert(listOf<Int>()) {
            lastElementShouldBe(0)
        }
    }

    @Test(expected = AssertionError::class)
    fun testContainsAll() {
        assert(listOf(1, 2, 3)) {
            containsAll(1, 8)
        }
    }

    @Test(expected = AssertionError::class)
    fun testContainsAllWithSet() {
        assert(setOf(1, 2, 3)) {
            containsAll(1, 8)
        }
    }

    @Test
    fun testShouldBeLess() {
        try {
            assert(listOf(1, 2, 3)) {
                shouldBe(listOf(1, 2, 3, 4))
            }
            Assert.fail("It shouldn't pass here")
        } catch (e: AssertionError) {
            assertTrue { "[4]" in e.message!! }
            assertTrue { "shorter" in e.message!! }
        }
    }

    @Test
    fun testShouldBeLonger() {
        try {
            assert(listOf(1, 2, 3)) {
                shouldBe(listOf(1, 2))
            }
            Assert.fail("It shouldn't pass here")
        } catch (e: AssertionError) {
            assertTrue { "[3]" in e.message!! }
            assertTrue { "longer" in e.message!! }
        }
    }

    @Test(expected = AssertionError::class)
    fun testShouldBeSetExtra() {
        assert(setOf(1, 2, 3)) {
            shouldBeSet(setOf(1, 2))
        }
    }

    @Test
    fun testShouldBeSetExact() {
        assert(setOf(1, 2, 3)) {
            shouldBeSet(setOf(1, 2, 3))
        }
    }

    @Test
    fun testShouldBeSetExactVararg() {
        assert(setOf(1, 2, 3)) {
            shouldBeSet(1, 2, 3)
        }
    }

    @Test(expected = AssertionError::class)
    fun testShouldBeSetMissing() {
        assert(setOf(1, 2, 3)) {
            shouldBeSet(setOf(1, 2, 3, 4))
        }
    }
}
