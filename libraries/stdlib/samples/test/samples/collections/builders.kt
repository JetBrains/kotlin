package samples.collections

import samples.*
import kotlin.test.*

@Suppress("ReplaceAssertBooleanWithAssertEquality", "UNRESOLVED_REFERENCE" /* See KT-30129 */) // TODO: remove this in 1.4
@RunWith(Enclosed::class)
class Builders {
    class Lists {
        @Sample
        fun buildListSample() {
            val x = buildList {
                add('a')
                add('b')
            }
            assertPrints(x, "[a, b]")

            val y = buildList(x.size + 1) {
                addAll(x)
                add('c')
            }
            assertPrints(y, "[a, b, c]")

            assertFails("initial capacity cannot be negative") {
                buildList(-1) {
                    add('a')
                }
            }
        }
    }

    class Sets {
        @Sample
        fun buildSetSample() {
            val x = buildSet {
                add('a')
                add('b')
            }
            assertPrints(x.sorted(), "[a, b]")

            val y = buildSet(x.size + 1) {
                addAll(x)
                add('c')
            }
            assertPrints(y.sorted(), "[a, b, c]")

            assertFails("initial capacity cannot be negative") {
                buildSet(-1) {
                    add('a')
                }
            }
        }
    }

    class Maps {
        @Sample
        fun buildMapSample() {
            val x = buildMap<Char, Int> {
                put('a', 1)
                put('b', 2)
            }
            assertPrints(x.toSortedMap(), "{a=1, b=2}")

            val y = buildMap<Char, Int>(x.size + 1) {
                putAll(x)
                put('c', 3)
            }
            assertPrints(y.toSortedMap(), "{a=1, b=2, c=3}")

            assertFails("initial capacity cannot be negative") {
                buildMap<Char, Int>(-1) {
                    put('a', 1)
                }
            }
        }
    }
}
