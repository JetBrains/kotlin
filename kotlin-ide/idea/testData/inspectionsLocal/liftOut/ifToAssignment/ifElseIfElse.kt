fun test(n: Int) {
    val a: String
    <caret>if (n == 1)
        a = "one"
    else if (n == 2)
        a = "two"
    else
        a = "three"
}