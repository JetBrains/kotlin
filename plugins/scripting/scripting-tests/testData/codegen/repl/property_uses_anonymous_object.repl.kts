
// SNIPPET

interface IV { val v: String }

val obj = object : IV { override val v = "OK" }

val sim = object : ArrayList<String>() {} // checking KT-74615 too

val y = obj.v

// EXPECTED: y == "OK"

// SNIPPET

val res = obj.v

sim.add(res)

// EXPECTED: res == "OK"
