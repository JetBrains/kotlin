// "Create type parameter 'T' in property 'a'" "false"
// ACTION: Create annotation 'T'
// ACTION: Create class 'T'
// ACTION: Create enum 'T'
// ACTION: Create interface 'T'
// ERROR: Unresolved reference: T
val a = fun() {
    val b: T<caret>
}