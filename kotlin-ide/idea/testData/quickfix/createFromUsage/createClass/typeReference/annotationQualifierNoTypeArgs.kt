// "Create annotation 'A'" "false"
// ACTION: Create class 'A'
// ACTION: Create object 'A'
// ACTION: Create interface 'A'
// ACTION: Create enum 'A'
// ACTION: Convert to block body
// ACTION: Remove explicit type specification
// ERROR: Unresolved reference: A
package p

internal fun foo(): <caret>A.B = throw Throwable("")