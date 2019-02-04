fun baz(args: Array<String>) {
    if (args.size > 4) {
        println("Too many")
    } else {
        println("Fine")
    }
}

class A {
    init {
        println("A::init")
    }

    fun f() {
        println("A::f")
    }
}