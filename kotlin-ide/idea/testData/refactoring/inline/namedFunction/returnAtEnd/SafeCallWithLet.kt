fun String.<caret>f(p: Int): Int {
    p.let { println(it) }
    println(p)
    println(p.let { 1 + it })
    return hashCode() * p
}

fun f(s: String?) {
    s?.f(1)
    s?.substring(1)?.f(2)
    val s1 = s?.f(3)
    val s2 = s?.substring(1)?.f(4)

    val s3 = s?.substring(1)
    s3?.f(2)
    s3?.f(3)?.let { println(it) }

    ""?.f(42)
}