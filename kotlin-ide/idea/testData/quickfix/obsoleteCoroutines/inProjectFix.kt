// "Fix experimental coroutines usages in the project" "true"
// WITH_RUNTIME
package migrate

import kotlin.coroutines.experimental.buildIterator
import kotlin.coroutines.experimental.buildSequence

fun main(args: Array<String>) {
    val one = <caret>buildSequence {
        yield(1)
    }

    val two = buildIterator {
        yield(1)
    }
}