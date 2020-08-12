class Chain

fun callRecei<caret>ver(chain: Chain, fn: Chain.() -> Chain): Chain {
    chain.fn()
    return chain.fn()
}

fun complicate(chain: Chain) {
    val vra = callReceiver(chain) { this }
    val vrb = callReceiver(chain) { Chain() }
}