// "Add default constructor to expect class" "false"
// ENABLE_MULTIPLATFORM
// ACTION: Make internal
// ACTION: Make private
// ACTION: Remove constructor call
// ERROR: Expected annotation class 'Foo' has no actual declaration in module light_idea_test_case for JVM
// ERROR: This class does not have a constructor
expect annotation class Foo

@Foo("")<caret>
fun bar() {}