fun f(a: Int = 1, b: String): String = a.toString() + b

class C(a: Int = 1, b: String) {
    val s: String = a.toString() + b
}
