// SNIPPET

var x: Int = { -> if (true) throw RuntimeException() else 5 }()

// EXPECTED_EXCEPTION: java.lang.RuntimeException

// SNIPPET

// the previous snippet instance exists even though its evaluation failed, so `x` keeps the JVM default
val res = x

// EXPECTED: res == 0
