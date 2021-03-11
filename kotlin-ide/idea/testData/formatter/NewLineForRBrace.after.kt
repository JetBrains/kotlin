enum class E1 { F, S, T }

enum class E2 { F, S, T }

enum class E3 {
    F, S, T
}

class C1 {}

class C2 {
    fun test() {}
}

object O1 {}

object O2 {
    fun test() {}
}

interface T1 {}

interface T2 {
    val some = 1
}

enum class E1 {
    EE1 {},
    EE2 {
        val some = 1
    }
}

fun e = fun(a: Int,
            b: String) {
}

/**
 *
 */
fun commented1() {}

/*
 */
fun commented2() {}

// Comment
fun commented3() {}

// TRAILING_COMMA: false