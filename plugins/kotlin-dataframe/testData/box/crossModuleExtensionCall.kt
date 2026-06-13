// DISABLE_TEST_UTILS
// CHECK_BYTECODE_LISTING
// MODULE: main
// FILE: schema.kt

package com.package1

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema

@DataSchema
data class Class1(
    val somethingId: Long,
)

// MODULE: test(main)
// FILE: test.kt
package com.package2

import com.package1.*
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

fun box(): String {
    val df: DataFrame<Class1> = dataFrameOf(
        Class1(1L),
        Class1(2L),
        Class1(3L),
        Class1(4L),
    )
    df.somethingId
    return "OK"
}
