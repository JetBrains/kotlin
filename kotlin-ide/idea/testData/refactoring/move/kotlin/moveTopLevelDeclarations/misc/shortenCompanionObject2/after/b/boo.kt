package b

import java.util.function.IntPredicate

interface Factory {
    operator fun invoke(i: Int): IntPredicate

    companion object {
        inline operator fun invoke(crossinline f: (Int) -> IntPredicate) = object : Factory {
            override fun invoke(i: Int) = f(i)
        }
    }
}

fun foo(): Factory = Factory { k ->
    IntPredicate { n -> n % k == 0 }
}