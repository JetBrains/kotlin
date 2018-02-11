// WITH_RUNTIME

fun test1() = (0..10).map { n ->
    object {
        override fun hashCode() = n
    }
}

fun test2() = (0..10).map { n ->
    object : Runnable {
        override fun run() {}
    }
}

abstract class Foo

fun test3() = (0..10).map { n ->
    object : Foo() {}
}

fun test4() = (0..10).map { n ->
    object : Foo(), Runnable {
        override fun run() {}
    }
}