// "Create parameter 'b'" "false"
// ERROR: Unresolved reference: b
// ACTION: Create property 'b'
// ACTION: Rename reference
// ACTION: Add 'a =' to argument

open class A(val a: Int) {

}

object B: A(<caret>b) {

}