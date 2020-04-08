// "Surround with null check" "true"

fun foo(arg: Int?, flag: Boolean) {
    when (flag) {
        true -> arg<caret>.hashCode()
    }
}