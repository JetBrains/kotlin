operator fun Int.invoke(): String {}

fun call(x: Int) {
    <selection>x()</selection>
}

// CALL: SimpleKtFunctionCallInfo: targetFunction = invoke(<receiver> : Int): String