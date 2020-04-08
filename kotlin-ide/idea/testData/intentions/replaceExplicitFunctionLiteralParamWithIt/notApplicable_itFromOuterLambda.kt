// IS_APPLICABLE: false

// See KT-12437: it from outer lambda
fun acceptLambda(p: Int, f: (Int) -> Int): Int = f(p)
fun use() {
    acceptLambda(1) {
        acceptLambda(2) { <caret>p2 -> it * 10 + p2 }
    }.hashCode()
}