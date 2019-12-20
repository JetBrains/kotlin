// "Add default constructor to expect class" "true"
// ENABLE_MULTIPLATFORM
// ERROR: Expected annotation class 'Foo' has no actual declaration in module light_idea_test_case for JVM

expect annotation class Foo

@Foo()<caret>
fun bar() {}