package samples.collections

import samples.*

@RunWith(Enclosed::class)
class Builders {
    class Lists {
        @Sample
        fun buildListSample() {
            val x = listOf('b', 'c')

            val y = buildList() {
                add('a')
                addAll(x)
                add('d')
            }

            assertPrints(y, "[a, b, c, d]")
        }

        @Sample
        fun buildListSampleWithCapacity() {
            val x = listOf('b', 'c')

            val y = buildList(x.size + 2) {
                add('a')
                addAll(x)
                add('d')
            }

            assertPrints(y, "[a, b, c, d]")
        }
    }

    class Sets {
        @Sample
        fun buildSetSample() {
            val x = setOf('a', 'b')

            val y = buildSet(x.size + 2) {
                add('b')
                addAll(x)
                add('c')
            }

            assertPrints(y, "[b, a, c]")
        }
    }

    class Maps {
        @Sample
        fun buildMapSample() {
            val x = mapOf('b' to 2, 'c' to 3)

            val y = buildMap<Char, Int>(x.size + 2) {
                put('a', 1)
                put('c', 0)
                putAll(x)
                put('d', 4)
            }

            assertPrints(y, "{a=1, c=3, b=2, d=4}")
        }
    }
}
