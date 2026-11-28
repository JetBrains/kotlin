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

    compareSchemas(
        df.select { age and weight },
        df.select { cols("age", "weight") },
        df.select { cols(pathOf("age"), pathOf("weight")) },
    )

    compareSchemas(
        df.select { name.firstName },
        df.select { name.cols("firstName") },
        df.select { "name".cols("firstName") },
        df.select { pathOf("name").cols("firstName") },
    )

    compareSchemas(
        df.select { name.firstName and name.lastName },
        df.select { name.cols(pathOf("firstName"), pathOf("lastName")) },
        df.select { "name".cols(pathOf("firstName"), pathOf("lastName")) },
        df.select { pathOf("name").cols(pathOf("firstName"), pathOf("lastName")) },
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
