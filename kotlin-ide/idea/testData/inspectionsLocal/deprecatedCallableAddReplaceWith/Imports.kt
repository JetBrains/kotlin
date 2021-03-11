package pack

import dependency.valueFromOtherPackage

<caret>@Deprecated("")
fun foo() {
    bar(valueFromOtherPackage)
}

fun bar(p: Int){}