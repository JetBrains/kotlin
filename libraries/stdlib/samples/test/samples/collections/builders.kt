package samples.collections

import samples.*

@RunWith(Enclosed::class)
class Builders {
    class Lists {
        @Sample
        fun buildListSample() {
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
            val x = setOf('b', 'c')

            val y = buildSet(x.size + 2) {
                add('a')
                addAll(x)
                add('d')
            }

            assertPrints(y, "[a, b, c, d]")
        }
    }

    class Maps {
        @Sample
        fun buildMapSample() {
            val x = mapOf('b' to 2, 'c' to 3)

            val y = buildMap<Char, Int>(x.size + 2) {
                put('a', 1)
                putAll(x)
                put('d', 4)
            }

            assertPrints(y, "{a=1, b=2, c=3, d=4}")
        }
    }
}
