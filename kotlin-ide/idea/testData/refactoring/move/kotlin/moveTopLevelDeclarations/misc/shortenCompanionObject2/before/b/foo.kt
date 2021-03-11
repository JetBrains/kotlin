package b

import java.util.function.IntPredicate

fun <caret>foo(): Factory = Factory { k ->
    IntPredicate { n -> n % k == 0 }
}
