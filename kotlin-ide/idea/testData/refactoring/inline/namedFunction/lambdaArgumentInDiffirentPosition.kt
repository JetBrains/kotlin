fun <X, Y> gener<caret>ic1(p: X, f: (X) -> Y): Y = f(p)
fun usage() {
    val a = generic1("abc") { x -> x.length }
    val b = generic1("abc", { x -> x.length })
    val c = generic1("abc") { it.length }
    val d = generic1("abc", { it.length })
}