// "Replace with 'newFun(n + listOf(s))'" "true"

import declaration.listOf
import declaration.newFun
import declaration.oldFun
import weird.collections.plus

fun foo() {
    <caret>newFun(listOf(2) + listOf(1))
}
