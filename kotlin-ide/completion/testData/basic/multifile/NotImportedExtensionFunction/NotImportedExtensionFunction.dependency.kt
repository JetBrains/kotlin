package second

fun String?.helloFun() {
}

fun String.helloWithParams(i : Int) : String {
    return ""
}

fun <T: CharSequence> T.helloFunGeneric() {
}

fun Int.helloFake() {
}

fun dynamic.helloDynamic() {
}