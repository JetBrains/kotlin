<warning descr="SSR">fun a(b: Int): Int { return b }</warning>

fun c(b: Int): Double { return <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is Int but Double was expected">b</error> }