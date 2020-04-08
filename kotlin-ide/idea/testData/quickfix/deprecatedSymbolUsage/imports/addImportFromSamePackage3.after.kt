// "Replace with 'newFun()'" "true"

import dependency.newFun
import dependency.oldFun

fun foo() {
    <caret>newFun()
}