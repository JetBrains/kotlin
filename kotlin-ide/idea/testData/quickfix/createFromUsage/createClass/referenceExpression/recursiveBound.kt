// "Create object 'Foo'" "false"
// ACTION: Create local variable 'Foo'
// ACTION: Create parameter 'Foo'
// ACTION: Create property 'Foo'
// ACTION: Split property declaration
// ACTION: Rename reference
// ERROR: Unresolved reference: Foo
open class Cyclic<E : Cyclic<E>>

fun test() {
    val c : Cyclic<*> = <caret>Foo
}