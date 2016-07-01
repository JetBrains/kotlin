package kotlin.jdk8.collections.test

import org.junit.Test
import kotlin.test.*
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.streams.*

class CollectionTest {

    val data = listOf("abc", "fo", "baar")

    @Test fun stream() {
        assertEquals(
                data.flatMap { it.asIterable() },
                data.stream()
                        .flatMap { it.chars().boxed().map { it.toChar() } }
                        .collect(Collectors.toList()))

        assertEquals(data, data.parallelStream().toList())
    }


    @Test fun removeIf() {
        val coll: MutableCollection<String> = data.toMutableList()
        assertTrue(coll.removeIf { it.length < 3 })
        assertEquals(listOf("abc", "baar"), coll as Collection<String>)
        assertFalse(coll.removeIf(Predicate { it.length > 4 }))
    }


}