package samples.collections

import samples.*
import kotlin.test.*
import java.util.*

@RunWith(Enclosed::class)
class Maps {

    class Instantiation {

        @Sample
        fun mapFromPairs() {
            val map = mapOf(1 to "x", 2 to "y", -1 to "zz")
            assertPrints(map, "{1=x, 2=y, -1=zz}")
        }

        @Sample
        fun mutableMapFromPairs() {
            val map = mutableMapOf(1 to "x", 2 to "y", -1 to "zz")
            assertPrints(map, "{1=x, 2=y, -1=zz}")

            map[1] = "a"
            assertPrints(map, "{1=a, 2=y, -1=zz}")
        }

        @Sample
        fun hashMapFromPairs() {
            val map: HashMap<Int, String> = hashMapOf(1 to "x", 2 to "y", -1 to "zz")
            assertPrints(map, "{-1=zz, 1=x, 2=y}")
        }

        @Sample
        fun linkedMapFromPairs() {
            val map: LinkedHashMap<Int, String> = linkedMapOf(1 to "x", 2 to "y", -1 to "zz")
            assertPrints(map, "{1=x, 2=y, -1=zz}")
        }

        @Sample
        fun sortedMapFromPairs() {
            val map = sortedMapOf(Pair("c", 3), Pair("b", 2), Pair("d", 1))
            assertPrints(map.keys, "[b, c, d]")
            assertPrints(map.values, "[2, 3, 1]")
        }

        @Sample
        fun emptyReadOnlyMap() {
            val map = emptyMap<String, Int>()
            assertTrue(map.isEmpty())

            val anotherMap = mapOf<String, Int>()
            assertTrue(map == anotherMap, "Empty maps are equal")
        }

        @Sample
        fun emptyMutableMap() {
            val map = mutableMapOf<Int, Any?>()
            assertTrue(map.isEmpty())

            map[1] = "x"
            map[2] = 1.05
            // Now map contains something:
            assertPrints(map, "{1=x, 2=1.05}")
        }

    }


    class Usage {

        @Sample
        fun getOrElse() {
            val map = mutableMapOf<String, Int?>()
            assertPrints(map.getOrElse("x") { 1 }, "1")

            map["x"] = 3
            assertPrints(map.getOrElse("x") { 1 }, "3")

            map["x"] = null
            assertPrints(map.getOrElse("x") { 1 }, "1")
        }

        @Sample
        fun getOrPut() {
            val map = mutableMapOf<String, Int?>()

            assertPrints(map.getOrPut("x") { 2 }, "2")
            // subsequent calls to getOrPut do not evaluate the default value
            // since the first getOrPut has already stored value 2 in the map
            assertPrints(map.getOrPut("x") { 3 }, "2")

            // however null value mapped to a key is treated the same as the missing value
            assertPrints(map.getOrPut("y") { null }, "null")
            // so in that case the default value is evaluated
            assertPrints(map.getOrPut("y") { 42 }, "42")
        }

        @Sample
        fun forOverEntries() {
            val map = mapOf("beverage" to 2.7, "meal" to 12.4, "dessert" to 5.8)

            for ((key, value) in map) {
                println("$key - $value") // prints: beverage - 2.7
                                         // prints: meal - 12.4
                                         // prints: dessert - 5.8
            }

        }
    }

    class Transformations {

        @Sample
        fun mapKeys() {
            val map1 = mapOf("beer" to 2.7, "bisquit" to 5.8)
            val map2 = map1.mapKeys { it.key.length }
            assertPrints(map2, "{4=2.7, 7=5.8}")

            val map3 = map1.mapKeys { it.key.take(1) }
            assertPrints(map3, "{b=5.8}")
        }

        @Sample
        fun mapValues() {
            val map1 = mapOf("beverage" to 2.7, "meal" to 12.4)
            val map2 = map1.mapValues { it.value.toString() + "$" }

            assertPrints(map2, "{beverage=2.7$, meal=12.4$}")
        }


        @Sample
        fun mapToSortedMap() {
            val map = mapOf(Pair("c", 3), Pair("b", 2), Pair("d", 1))
            val sorted = map.toSortedMap()
            assertPrints(sorted.keys, "[b, c, d]")
            assertPrints(sorted.values, "[2, 3, 1]")
        }

        @Sample
        fun mapToSortedMapWithComparator() {
            val map = mapOf(Pair("abc", 1), Pair("c", 3), Pair("bd", 4), Pair("bc", 2))
            val sorted = map.toSortedMap(compareBy<String> { it.length }.thenBy { it })
            assertPrints(sorted.keys, "[c, bc, bd, abc]")
        }

        @Sample
        fun mapToProperties() {
            val map = mapOf("x" to "value A", "y" to "value B")
            val props = map.toProperties()

            assertPrints(props.getProperty("x"), "value A")
            assertPrints(props.getProperty("y", "fail"), "value B")
            assertPrints(props.getProperty("z", "fail"), "fail")
        }

    }

}

