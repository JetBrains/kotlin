fun a() {
    when {
        true && true -> {
        }
        false        -> {
        }
        else         -> {
        }
    }
}

fun b() {
    val sealed: Sealed = Sealed.Foo()
    when (val a = sealed) {
        is Sealed.Foo -> println("Foo")
        is Sealed.A   -> println("A")
        Sealed.A()    -> print(1)
        else          -> {
        }
    }
}

fun c() {
    val sealed: Sealed = Sealed.Foo()
    when (sealed) {
        is Sealed.Foo -> println("Foo")
        is Sealed.A   -> println("A")
        Sealed.A()    -> print(1)
        else          -> {
        }
    }
}

// SET_TRUE: ALIGN_IN_COLUMNS_CASE_BRANCH