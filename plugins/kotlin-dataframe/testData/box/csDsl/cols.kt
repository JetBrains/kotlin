// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { cols(name, age, city, weight, isHappy) },
        df.select { all() },
    )

    compareSchemas(
        df.select { cols(name) },
        df.select { name },
    )

    val df2 = dataFrameOf(
        "test" to columnOf("a"),
        "group" to columnOf(
            "myCol" to columnOf(1)
        ),
    )

    df2.cast<Any>().select { cols("group"["myCol"] named "newName") }.let {
        checkExactType<Any?>(it.newName[0])
    }
    return "OK"
}
