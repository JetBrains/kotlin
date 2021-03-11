package lambdaParameters

fun fun1(p: String, f: (String) -> Int) {
    //Breakpoint!
    f(p)
}

inline fun fun2(p: String, crossinline f: (String) -> Int) {
    //Breakpoint!
    f(p)
}

fun main(args: Array<String>) {
    fun1("abc", { x -> x.length })
    fun2("abc", { x -> x.length })
}

// EXPRESSION: f("abc")
// RESULT: 3: I

// EXPRESSION: f("abc")
// RESULT: Evaluation of 'crossinline' lambdas is not supported