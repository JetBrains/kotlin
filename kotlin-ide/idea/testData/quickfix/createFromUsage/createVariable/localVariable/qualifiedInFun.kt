// "Create local variable 'foo'" "false"
// ACTION: Create extension property 'A.foo'
// ACTION: Create member property 'A.foo'
// ACTION: Create property 'foo' as constructor parameter
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

class A

fun test(a: A) {
    val t: Int = a.<caret>foo
}