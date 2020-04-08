// "Import" "false"
// ERROR: Unresolved reference: Some
// ACTION: Create function 'Some'
// ACTION: Rename reference
// ACTION: Add 'ctor =' to argument

package p1

import p2.receiveSomeCtor

fun some() {
    receiveSomeCtor(::Some<caret>)
}