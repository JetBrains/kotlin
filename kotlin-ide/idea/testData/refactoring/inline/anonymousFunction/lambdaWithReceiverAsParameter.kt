class Chain

fun complicate(chain: Chain) {
    val vra = (fu<caret>n(chain: Chain, fn: Chain.() -> Chain): Chain {
        chain.fn()
        return chain.fn()
    })(chain){ this.also { println(it) } }
}