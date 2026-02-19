// SNIPPET

@Suppress("UNCHECKED_CAST")
fun <S> materialize(): S = "OK" as S

class C { val v: String = materialize() }

val y = C().v

// EXPECTED: y == "OK"

// SNIPPET

val res = C().v

// EXPECTED: res == "OK"
