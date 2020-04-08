// IS_APPLICABLE: false

interface B {
}

annotation class Ann

@Ann
val <caret>a = object : B {
}