package foo

fun box(): String {
    val a = 23.{ Int.(a: Int) -> a * a + this }(3)
    if (a != 32) return "a != 32, a = $a";

    return "OK";
}
