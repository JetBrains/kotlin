
// SNIPPET

val x = "O"
class C(val x: String = "K")

val res = "$x${C().x}"

// EXPECTED: res == "OK"
