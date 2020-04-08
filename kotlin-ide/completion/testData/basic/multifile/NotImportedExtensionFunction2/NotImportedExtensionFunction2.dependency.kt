package second

fun (() -> Unit)?.helloFun1() {
}

fun Function0<Unit>.helloFun2() {
}

fun @ExtensionFunctionType Function1<String, Unit>.helloFun3() {
}

fun Function1<String, Unit>.helloFun4() {
}

fun Any.helloAny() {
}
