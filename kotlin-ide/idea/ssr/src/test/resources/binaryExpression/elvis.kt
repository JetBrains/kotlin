fun main() {
    var a: Int? = 1
    val b = 2
    print(<warning descr="SSR">(<warning descr="SSR">a ?: 0</warning>)</warning> + b)
}