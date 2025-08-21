// SNIPPET

data class Bar(val name: String)

// SNIPPET

val b = Bar("OK")
val res = b.name

// EXPECTED: res == OK

// SNIPPET

val c = b.copy(name = "another")
val res2 = c.component1()

// EXPECTED: res2 == another

// SNIPPET

val res3 = b.equals(c)

// EXPECTED: res3 == false

// SNIPPET

val d = c.copy(name = "OK")
val res4 = b.equals(d)

// EXPECTED: res4 == true
