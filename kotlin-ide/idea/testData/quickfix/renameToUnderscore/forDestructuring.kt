// "Rename to _" "true"

data class A(val x: String, val y: Int)

fun bar(z: List<A>) {
    for ((x<caret>, y) in z) {
        y.hashCode()
    }
}
