// "Optimize imports" "false"
// ACTION: Introduce import alias

import test.Other.HELLO<caret>

val ONE = HELLO

enum class MyEnum {
    HELLO
}