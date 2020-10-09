fun resolveMe() {
    receive(functionWithLazyBody())
}

fun receive(value: String){}

fun functionWithLazyBody() = "42"