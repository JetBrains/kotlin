fun foo(list: List<String>) {
    if (true)<caret> println(list)

    // printing...
    println("hi")

    for (l in list) println(l)

    // printing...
    println("hi")
}

fun println(a: Any) {}