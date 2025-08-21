
// SNIPPET

class A

class B

fun A.x1() = "O"
fun B.x1() = "K"

val x = A().x1() + B().x1()

// EXPECTED: x == "OK"

// SNIPPET

val y = B().x1() + A().x1()

// EXPECTED: y == "KO"
