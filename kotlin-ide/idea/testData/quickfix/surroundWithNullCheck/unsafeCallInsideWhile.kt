// "Surround with null check" "true"

fun foo(arg: Int?, flag: Boolean) {
    while (flag) arg<caret>.hashCode()
}