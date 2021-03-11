// "Create function 'foo'" "false"
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

package p

import p.<caret>foo

fun test() {

}