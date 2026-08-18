// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "group1" to columnOf(
            "col1" to columnOf(1, 2, 3),
            "col2" to columnOf(3, 4, 5)
        ),
        "group2" to columnOf(
            "col3" to columnOf(6, 7, 8),
            "col4" to columnOf(1, 2, 3),
            "col5" to columnOf("a", "b", "c")
        ),
        "group3" to columnOf(
            "col1" to columnOf(1, 2, 3),
            "col3" to columnOf(6, 7, 8),
            "col5" to columnOf("a", "b", "c")
        ),
    )
    val elements = df.merge { group1 and group2 and group3 }.into("frames")

    elements[0].frames[0].let { row ->
        checkExactType<Int?>(row.col1)
        checkExactType<Int?>(row.col2)
        checkExactType<Int?>(row.col3)
        checkExactType<Int?>(row.col4)
        checkExactType<String?>(row.col5)
    }
    elements.compareSchemas(strict = true)

    return "OK"
}
