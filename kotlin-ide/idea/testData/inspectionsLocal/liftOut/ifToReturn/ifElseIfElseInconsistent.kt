fun test(n: Int): String {
    val a: String
    <caret>if (n == 1)
        return "one"
    else if (n == 2)
        return "two"
    else
        a = "three"
    return a
}