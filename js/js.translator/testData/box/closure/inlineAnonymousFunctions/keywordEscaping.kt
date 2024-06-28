// ISSUE: KT-66898
class Test(private val param1: (String) -> String) {
    // or any other `<reserved-keyword>: String`
    constructor(void: String) : this({ x -> x + void })

    fun eval(value: String): String {
        return param1("O")
    }
}

fun box(): String {
    return Test("K").eval("O")
}
