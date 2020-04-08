class Foo {

    fun method(b: Boolean,
               elementComputable: Computable<String>,
               processingContext: Any): WeighingComparable<String, ProximityLocation> {
        return weigh(WEIGHER_KEY, elementComputable, ProximityLocation())
    }

    companion object {
        fun <T, Loc> <caret>weigh(
                key: Key<out Weigher<T, Loc>>?, element: Computable<T>, location: Loc
        ): WeighingComparable<T, Loc> {
            return WeighingComparable(element, location, Array<Weigher<*, *>>(0) { null!! })
        }

        val WEIGHER_KEY: Key<ProximityWeigher>? = null
    }
}

abstract class ProximityWeigher : Weigher<String, ProximityLocation>()

class ProximityLocation

class Key<P>

open class Weigher<A, B>

class Computable<O>

class WeighingComparable<T, Loc>(element: Computable<T>, location: Loc, weighers: Array<Weigher<*, *>>) : Comparable<WeighingComparable<T, Loc>> {

    override fun compareTo(other: WeighingComparable<T, Loc>): Int {
        return 0
    }

    private fun getWeight(index: Int): Comparable<*>? {
        return null
    }
}
