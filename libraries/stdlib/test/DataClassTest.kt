package test.dataclass

import org.junit.Test
import kotlin.test.*
import java.io.Serializable

/**
 */
class DataClassTest {
    Test fun dataClass() {
        val p = Person("James", 43)
        println("Got $p")
        assertEquals("Person(name=James, age=43)", "$p", "toString")

        val (a, b) = p
        assertEquals("James", a, "a")
        assertEquals(43, b, "b")

        assertEquals(Person("James", 43), Person("James", 43), "person equals")

        // TODO not implemented yet
        // assertTrue(Person("ZZZ", 21) > Person("AAA", 21), "person a > b")
    }
}

data class Person(val name: String, val age: Int)