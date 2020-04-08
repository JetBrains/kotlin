class Person(val name: String)

val reader = { p: Person -> p.name<caret> }