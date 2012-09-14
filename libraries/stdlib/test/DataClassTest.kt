package test.dataclass

import org.junit.Test
import kotlin.test.*

/**
 */
class DataClassTest {
    Test fun dataClass() {
        val p = Person("James", 43)
        // TODO no nice toString() yet
        println("Got $p")

        val (a, b) = p
        println("a: $a, b: $b")
        assertEquals("James", a, "a")
        assertEquals(43, b, "b")

        // TODO not implemented yet
        // assertEquals(Person("James", 43), Person("James", 43), "person equals")
        // assertTrue(Person("ZZZ", 21) > Person("AAA", 21), "person a > b")
    }
}

data class Person(val name: String, val age: Int)