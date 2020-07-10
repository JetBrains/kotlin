// above function
fun String.<caret>f(p: Int): Int { /* after open brace */ // after open brace
    /* above println */
    println(p) // after println
    // above return
    return hashCode() * p // after return
    // below return
    // at the end
} /* after function 1 */   /* after function 2 */ // after function 3

fun f(s: String?) {
    s?.f(1)
    val s1 = s?.f(3)
    val s2 = s?.substring(1)?.f(4)

    val s3 = s?.substring(1)
    s3?.f(2)
    s3?.f(3)


    "".f(42)
}

fun doo(s: String?) {
    s?.substring(1)?.f(2)
}