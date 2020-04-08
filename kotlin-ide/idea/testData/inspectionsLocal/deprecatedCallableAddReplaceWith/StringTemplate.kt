<caret>@Deprecated("")
fun foo(p: Int) {
    bar("$p ${p + 1} $0")
}

fun bar(s: String){}