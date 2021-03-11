// "Safe delete 'MyObj'" "false"
// ACTION: Create test
// ACTION: Rename file to MyObj.kt

import MyObj.foo

object <caret>MyObj {
    fun foo() {}
}