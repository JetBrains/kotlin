// SNIPPET

// lambda classes from different snippets must not clash
val l1 = { -> }
21

// EXPECTED: <res> == 21

// SNIPPET

val l2 = { -> }
22

// EXPECTED: <res> == 22
