// "Create type parameter 'T' in property 'a'" "false"
// ACTION: Convert property initializer to getter
// ACTION: Convert to lazy property
// ACTION: Create annotation 'T'
// ACTION: Create class 'T'
// ACTION: Create enum 'T'
// ACTION: Create interface 'T'
// ACTION: Remove explicit type specification
// ERROR: Unresolved reference: T
val a: <caret>T? = null