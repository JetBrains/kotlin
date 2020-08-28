class Chain

fun complicate(chain: Chain) {
    val vra = (fun<caret>(chain: Chain, fn: Chain.() -> Chain): Chain {
        chain.fn()
        return chain.fn()
    })(chain, { Chain().also { println(it) } })
}