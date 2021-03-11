// WITH_RUNTIME
class Chain

fun complicate(chain: Chain) {
    val vra = (fu<caret>n Chain.(): Chain {
        return also { println(it) }
    })(chain)
}