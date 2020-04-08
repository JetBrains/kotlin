fun test(n: Int): Array<String> {
    var x: Array<String> = arrayOfNulls<String>(1) as Array<String>

    x[0] = <caret>if (n > 5) "A" else "B"

    return x
}
