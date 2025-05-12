
// SNIPPET

var x = "_"

val y by lazy { x }

// SNIPPET

x = "OK"

val z = y

// EXPECTED: z == "OK"
