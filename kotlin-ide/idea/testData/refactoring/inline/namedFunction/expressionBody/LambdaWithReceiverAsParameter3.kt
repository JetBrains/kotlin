class Chain

fun callRecei<caret>ver(chain: Chain, fn: Chain.() -> Chain): Chain {
    println(chain.fn())
    print(chain.fn())
    return chain
}

fun complicate(chain: Chain) {
    val vra = callReceiver(chain) { this }
    val vrb = callReceiver(chain) { Chain() }
}