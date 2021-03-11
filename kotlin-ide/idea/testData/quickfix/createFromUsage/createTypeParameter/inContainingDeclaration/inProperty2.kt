// "Create type parameter 'T' in property 'a'" "false"
// ACTION: Convert property initializer to getter
// ACTION: Convert to lazy property
// ACTION: Create annotation 'T'
// ACTION: Create class 'T'
// ACTION: Create enum 'T'
// ACTION: Create interface 'T'
// ACTION: Create type parameter 'T' in class 'Test'
// ACTION: Move to constructor
// ACTION: Remove explicit type specification
// ERROR: Unresolved reference: T
class Test {
    val a: <caret>T? = null
}