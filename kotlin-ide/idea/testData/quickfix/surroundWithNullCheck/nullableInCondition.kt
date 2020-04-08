// "Surround with null check" "true"
// WITH_RUNTIME

fun foz(arg: String?) {
    if (arg<caret>.isNotEmpty()) {

    }
}