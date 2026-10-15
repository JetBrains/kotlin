// SNIPPET

// KT-6843
data class Person(val name: String)

// SNIPPET

var x: String? = "hello"

// SNIPPET

val y = x?.let { Person(it) }

// EXPECTED: y == Person(name=hello)
