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
        fun sortedMapWithComparatorFromPairs() {
            val map = sortedMapOf(compareBy<String> { it.length }.thenBy { it }, Pair("abc", 1), Pair("c", 3), Pair("bd", 4), Pair("bc", 2))
            assertPrints(map.keys, "[c, bc, bd, abc]")
            assertPrints(map.values, "[3, 2, 4, 1]")
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

        @Sample
        fun emptyHashMap() {
            val map = hashMapOf<Int, Any?>()
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


        @Sample
        fun mapIsNullOrEmpty() {
            val nullMap: Map<String, Any>? = null
            assertTrue(nullMap.isNullOrEmpty())

            val emptyMap: Map<String, Any>? = emptyMap<String, Any>()
            assertTrue(emptyMap.isNullOrEmpty())

            val map: Map<Char, Int>? = mapOf('a' to 1, 'b' to 2, 'c' to 3)
            assertFalse(map.isNullOrEmpty())
        }

        @Sample
        fun mapOrEmpty() {
            val nullMap: Map<String, Any>? = null
            assertPrints(nullMap.orEmpty(), "{}")

            val map: Map<Char, Int>? = mapOf('a' to 1, 'b' to 2, 'c' to 3)
            assertPrints(map.orEmpty(), "{a=1, b=2, c=3}")
        }

        @Sample
        fun mapIfEmpty() {
            val emptyMap: Map<String, Int> = emptyMap()

            val emptyOrNull = emptyMap.ifEmpty { null }
            assertPrints(emptyOrNull, "null")

            val emptyOrDefault: Map<String, Any> = emptyMap.ifEmpty { mapOf("s" to "a") }
            assertPrints(emptyOrDefault, "{s=a}")

            val nonEmptyMap = mapOf("x" to 1)
            val sameMap = nonEmptyMap.ifEmpty { null }
            assertTrue(nonEmptyMap === sameMap)
        }

        @Sample
        fun containsValue() {
            val map: Map<String, Int> = mapOf("x" to 1, "y" to 2)

            // member containsValue is used
            assertTrue(map.containsValue(1))

            // extension containsValue is used when the argument type is a supertype of the map value type
            assertTrue(map.containsValue(1 as Number))
            assertTrue(map.containsValue(2 as Any))

            assertFalse(map.containsValue("string" as Any))

            // map.containsValue("string") // cannot call extension when the argument type and the map value type are unrelated at all
        }

        @Sample
        fun containsKey() {
            val map: Map<String, Int> = mapOf("x" to 1)

            assertTrue(map.contains("x"))
            assertTrue("x" in map)

            assertFalse(map.contains("y"))
            assertFalse("y" in map)
        }

        @Sample
        fun mapIsNotEmpty() {
            fun totalValue(statisticsMap: Map<String, Int>): String =
                when {
                    statisticsMap.isNotEmpty() -> {
                        val total = statisticsMap.values.sum()
                        "Total: [$total]"
                    }
                    else -> "<No values>"
                }

            val emptyStats: Map<String, Int> = mapOf()
            assertPrints(totalValue(emptyStats), "<No values>")

            val stats: Map<String, Int> = mapOf("Store #1" to 1247, "Store #2" to 540)
            assertPrints(totalValue(stats), "Total: [1787]")
        }

        @Sample
        fun getValueWithoutDefault() {
            val map = mapOf(1 to "One", 2 to "Two")
            assertFailsWith<NoSuchElementException> { map.getValue(3) }
            assertPrints(map.getValue(1), "One")
        }

        @Sample
        fun getValueWithDefault() {
            val mapWithDefault = mapOf(1 to "One", 2 to "Two").withDefault { key ->
                when {
                    key == 0 -> "Zero"
                    key > 0 -> "Positive"
                    else -> "Negative"
                }
            }
            val actual = (-1..3).associateWith { mapWithDefault.getValue(it) }
            assertPrints(actual, "{-1=Negative, 0=Zero, 1=One, 2=Two, 3=Positive}")
        }

        @Sample
        fun getValueWithReplacedDefault() {
            val mapWithDefault = mapOf(1 to "One", 2 to "Two").withDefault { _ -> "Other" }
            assertPrints(mapWithDefault.getValue(0), "Other")
            val mapWithReplacedDefault = mapWithDefault.withDefault { _ -> "Unknown" }
            assertPrints(mapWithReplacedDefault.getValue(0), "Unknown")
        }

        @Sample
        fun changesToMutableMapWithDefaultPropagateToUnderlyingMap() {
            val mutableMap = mutableMapOf(1 to "One", 2 to "Two")
            val mutableMapWithDefault = mutableMap.withDefault { _ -> "Other" }
            assertPrints(mutableMapWithDefault.getValue(0), "Other")
            mutableMapWithDefault[0] = "Zero"
            assertPrints(mutableMapWithDefault.getValue(0), "Zero")
            assertPrints(mutableMap.getValue(0), "Zero")
        }

    }

    class Filtering {

        @Sample
        fun filterKeys() {
            val originalMap = mapOf("key1" to 1, "key2" to 2, "something_else" to 3)

            val filteredMap = originalMap.filterKeys { it.contains("key") }
            assertPrints(filteredMap, "{key1=1, key2=2}")
            // original map has not changed
            assertPrints(originalMap, "{key1=1, key2=2, something_else=3}")

            val nonMatchingPredicate: (String) -> Boolean = { it == "key3" }
            val emptyMap = originalMap.filterKeys(nonMatchingPredicate)
            assertPrints(emptyMap, "{}")
        }

        @Sample
        fun filterValues() {
            val originalMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3)

            val filteredMap = originalMap.filterValues { it >= 2 }
            assertPrints(filteredMap, "{key2=2, key3=3}")
            // original map has not changed
            assertPrints(originalMap, "{key1=1, key2=2, key3=3}")

            val nonMatchingPredicate: (Int) -> Boolean = { it == 0 }
            val emptyMap = originalMap.filterValues(nonMatchingPredicate)
            assertPrints(emptyMap, "{}")
        }

        @Sample
        fun filterTo() {
            val originalMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3)
            val destinationMap = mutableMapOf("key40" to 40, "key50" to 50)

            val filteredMap = originalMap.filterTo(destinationMap) { it.value < 3 }

            //destination map is updated with filtered items from the original map
            assertTrue(destinationMap === filteredMap)
            assertPrints(destinationMap, "{key40=40, key50=50, key1=1, key2=2}")
            // original map has not changed
            assertPrints(originalMap, "{key1=1, key2=2, key3=3}")

            val nonMatchingPredicate: ((Map.Entry<String, Int>)) -> Boolean = { it.value == 0 }
            val anotherDestinationMap = mutableMapOf("key40" to 40, "key50" to 50)
            val filteredMapWithNothingMatched = originalMap.filterTo(anotherDestinationMap, nonMatchingPredicate)
            assertPrints(filteredMapWithNothingMatched, "{key40=40, key50=50}")
        }

        @Sample
        fun filter() {
            val originalMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3)

            val filteredMap = originalMap.filter { it.value < 2 }

            assertPrints(filteredMap, "{key1=1}")
            // original map has not changed
            assertPrints(originalMap, "{key1=1, key2=2, key3=3}")

            val nonMatchingPredicate: ((Map.Entry<String, Int>)) -> Boolean = { it.value == 0 }
            val emptyMap = originalMap.filter(nonMatchingPredicate)
            assertPrints(emptyMap, "{}")
        }

        @Sample
        fun filterNotTo() {
            val originalMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3)
            val destinationMap = mutableMapOf("key40" to 40, "key50" to 50)

            val filteredMap = originalMap.filterNotTo(destinationMap) { it.value < 3 }
            //destination map instance has been updated
            assertTrue(destinationMap === filteredMap)
            assertPrints(destinationMap, "{key40=40, key50=50, key3=3}")
            // original map has not changed
            assertPrints(originalMap, "{key1=1, key2=2, key3=3}")

            val anotherDestinationMap = mutableMapOf("key40" to 40, "key50" to 50)
            val matchAllPredicate: ((Map.Entry<String, Int>)) -> Boolean = { it.value > 0 }
            val filteredMapWithEverythingMatched = originalMap.filterNotTo(anotherDestinationMap, matchAllPredicate)
            assertPrints(filteredMapWithEverythingMatched, "{key40=40, key50=50}")
        }

        @Sample
        fun filterNot() {
            val originalMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3)

            val filteredMap = originalMap.filterNot { it.value < 3 }
            assertPrints(filteredMap, "{key3=3}")
            // original map has not changed
            assertPrints(originalMap, "{key1=1, key2=2, key3=3}")

            val matchAllPredicate: ((Map.Entry<String, Int>)) -> Boolean = { it.value > 0 }
            val emptyMap = originalMap.filterNot(matchAllPredicate)
            assertPrints(emptyMap, "{}")
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
        fun mapNotNull() {
            val map = mapOf("Alice" to 20, "Tom" to 13, "Bob" to 18)
            val adults = map.mapNotNull { (name, age) -> name.takeIf { age >= 18 } }

            assertPrints(adults, "[Alice, Bob]")
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

        @Sample
        fun mapToList() {
            val peopleToAge = mapOf("Alice" to 20, "Bob" to 21)
            assertPrints(
                peopleToAge.map { (name, age) -> "$name is $age years old" },
                "[Alice is 20 years old, Bob is 21 years old]"
            )
            assertPrints(peopleToAge.map { it.value }, "[20, 21]")
        }

        @Sample
        fun flatMap() {
            val map = mapOf("122" to 2, "3455" to 3)
            assertPrints(map.flatMap { (key, value) -> key.take(value).toList() }, "[1, 2, 3, 4, 5]")
        }
    }

    class CoreApi {
        @Sample
        fun size() {
            assertEquals(0, emptyMap<Int, Int>().size)

            val mutableMap = mutableMapOf(1 to "one", 2 to "two")
            assertEquals(2, mutableMap.size)

            mutableMap[3] = "three"
            assertEquals(3, mutableMap.size)
        }

        @Sample
        fun isEmpty() {
            assertTrue(emptyMap<Int, Int>().isEmpty())
            assertFalse(mapOf(1 to 2).isEmpty())
        }

        @Sample
        fun get() {
            val map = mapOf(1 to "one", 2 to "two")

            assertEquals("two", map[2])
            assertNull(map[3])
        }

        @Sample
        fun put() {
            val map = mutableMapOf(1 to "one", 2 to "two")
            map[1] = "*ONE*"
            assertPrints(map, "{1=*ONE*, 2=two}")

            map[3] = "tree"
            assertPrints(map, "{1=*ONE*, 2=two, 3=tree}")

            assertPrints(map.put(3, "three"), "tree")
            assertPrints(map.put(4, "four"), "null")
        }

        @Sample
        fun putAll() {
            val map = mutableMapOf(1 to "one", 2 to "two")
            map.putAll(mapOf(3 to "three", 4 to "four", 1 to "_ONE_"))

            assertPrints(map, "{1=_ONE_, 2=two, 3=three, 4=four}")
        }

        @Sample
        fun clear() {
            val map = mutableMapOf(1 to "one", 2 to "two")
            assertPrints(map, "{1=one, 2=two}")

            map.clear()
            assertPrints(map, "{}")
        }

        @Sample
        fun remove() {
            val map = mutableMapOf(1 to "one", 2 to "two")

            assertEquals("one", map.remove(1))
            assertPrints(map, "{2=two}")

            // There's no value for key=1 anymore
            assertNull(map.remove(1))
        }

        @Sample
        fun containsKey() {
            val map = mapOf(1 to "one", 2 to "two")
            assertTrue(map.containsKey(1))
            assertFalse(map.containsKey(-1))
        }

        @Sample
        fun containsValue() {
            val map = mapOf(1 to "one", 2 to "two")
            assertTrue(map.containsValue("one"))
            assertFalse(map.containsValue("1"))
        }

        @Sample
        fun keySet() {
            val map = mapOf(1 to "one", 2 to "two")
            assertPrints(map.keys, "[1, 2]")
        }

        @Sample
        fun keySetMutable() {
            val map = mutableMapOf(1 to "one", 2 to "two")
            val keys = map.keys
            assertPrints(keys, "[1, 2]")

            keys.remove(1)
            assertPrints(keys, "[2]")
            assertPrints(map, "{2=two}")

            assertFailsWith<UnsupportedOperationException> { keys.add(3) }
        }

        @Sample
        fun valueSet() {
            val map = mapOf(1 to "one", 2 to "two", -2 to "two")
            val values = map.values
            assertPrints(values, "[one, two, two]")
        }

        @Sample
        fun valueSetMutable() {
            val map = mutableMapOf(1 to "one", 2 to "two", -2 to "two")
            val values = map.values
            assertPrints(values, "[one, two, two]")

            values.removeAll { it == "two" }
            assertPrints(values, "[one]")
            assertPrints(map, "{1=one}")

            assertFailsWith<UnsupportedOperationException> { values.add("v") }
        }

        @Sample
        fun entrySet() {
            val map = mapOf(1 to "one", 2 to "two")
            assertPrints(map.entries, "[1=one, 2=two]")
        }

        @Sample
        fun entrySetMutable() {
            val map = mutableMapOf(1 to "one", 2 to "two")
            val entries = map.entries
            assertPrints(entries, "[1=one, 2=two]")

            entries.first().setValue("*ONE*")
            assertPrints(entries, "[1=*ONE*, 2=two]")

            entries.clear()
            assertTrue(map.isEmpty())
        }
    }
}

