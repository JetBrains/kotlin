fun foo(list: List<String>) {
    if (true) println(list)

    // printing...
    println("hi")

    for (l in list)<caret> println(l)

    // printing...
    println("hi")
}

fun println(a: Any) {}