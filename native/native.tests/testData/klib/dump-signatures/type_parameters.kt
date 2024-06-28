// FIR_IDENTICAL

package type_parameters

private fun consume(anything: Any?) {
    anything.toString()
}

fun <P1 : CharSequence, P2 : Collection<*>, P3 : List<*>> function(p1: P1) {
    consume(p1)

    fun local1(p2: P2?) { consume(p2) }
    local1(null)

    fun <Q1 : Set<*>> local2(q1: Q1?) { consume(q1) }
    local2(null)

    class Local1(val p3: P3?)
    consume(Local1(null))

    class Local2<Q2 : Appendable>(val q2: Q2?)
    consume(Local2(null))
}

val <P1 : Number> P1.property: P1
    get() {
        fun local1(p1: P1?) { consume(p1) }
        local1(null)

        fun <Q1 : MutableCollection<*>> local2(q1: Q1?) { consume(q1) }
        local2(null)

        class Local1(val p1: P1?)
        consume(Local1(null))

        class Local2<Q2 : MutableSet<*>>(val q2: Q2?)
        consume(Local2(null))

        return this
    }

class Class<P1, P2>(val p1: P1) {
    class Nested<Q1>(val q1: Q1)
    inner class Inner<Q2>(val p2: P2, val q2: Q2)
}

open class OpenClass<P1>(val p1: P1?)
class FinalClass<P2>(val p2: P2) : OpenClass<MutableMap<*, *>>(null)
