var a: String? = "Hello world"

var b: String? = "Hello world"

var c: String? = "Hello other world"

val d = <warning descr="SSR">a == b</warning>

val e = <warning descr="SSR">a?.equals(b) ?: (b === null)</warning>

val f = a == c

val g = c == b

val h = a?.equals(c) ?: (b === null)

val i = c?.equals(b) ?: (b === null)

val j = a?.equals(b) ?: (c === null)

val k = a?.equals(b)

val l = a === b

val m = a != b