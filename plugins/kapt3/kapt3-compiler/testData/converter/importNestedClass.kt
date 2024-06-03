// CORRECT_ERROR_TYPES
// FILE: a.kt

package kapt

class Test {
    class Nested {
        class NestedNested
    }

    object NestedObject

    enum class NestedEnum {
        BLACK, WHITE
    }
}

// FILE: b.kt

package usage

import kapt.Test.Nested
import kapt.Test.NestedObject
import kapt.Test.Nested.NestedNested
import kapt.Test.NestedEnum
import kapt.Test.NestedEnum.BLACK

fun test() {
    Nested()
    NestedObject
    NestedNested()
    NestedEnum.WHITE
    BLACK
}
