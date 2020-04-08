// "Replace with 's.extension().newFun()'" "true"

import dependency.newFun
import dependency.oldFun
import dependency2.extension

fun foo() {
    "a".extension().<caret>newFun()
}