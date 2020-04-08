// "Fix experimental coroutines usage" "true"
// ERROR: Using 'buildIterator(noinline suspend SequenceScope<T>.() -> Unit): Iterator<T>' is an error. Use 'iterator { }' function instead.
// WITH_RUNTIME
import kotlin.coroutines.<caret>experimental.buildIterator

fun main(args: Array<String>) {
    val lazySeq = buildIterator<Int> {
    }
}