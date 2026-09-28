// SNIPPET

// KT-5622
arrayOf(1, 2)[0]

// EXPECTED: <res> == 1

// SNIPPET

arrayOf(1, 2).get(1)

// EXPECTED: <res> == 2

// SNIPPET

intArrayOf(1)[0]

// EXPECTED: <res> == 1

// SNIPPET

Integer.valueOf(42)

// EXPECTED: <res> == 42

// SNIPPET

java.lang.Long.MIN_VALUE as Any

// EXPECTED: <res> == -9223372036854775808
