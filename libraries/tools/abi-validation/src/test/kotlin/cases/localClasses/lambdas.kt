package cases.localClasses


class L {
    internal fun a(lambda: () -> Unit) = lambda()

    internal inline fun inlineLambda() {
        a {
            println("OK")
        }
    }
}

fun box() {
    L().inlineLambda()
}


// TODO: inline lambda from stdlib