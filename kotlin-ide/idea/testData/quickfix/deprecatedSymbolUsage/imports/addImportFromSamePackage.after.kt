// "Replace with 's.newFun()'" "true"

import dependency.newFun
import dependency.oldFun

fun foo() {
    "a".<caret>newFun()
}