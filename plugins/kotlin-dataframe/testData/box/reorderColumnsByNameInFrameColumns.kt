// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("name", "weight", "city", "age")(
        "Alice", 54, "London", 15,
        "Bob", 87, "Dubai", 45,
        "Charlie", null, "London", 20,
    )
        .group { weight and age }.into("info")
        .groupBy { city }.into("g")
        .add("b") { 1 }

    df.reorderColumnsByName(inFrameColumns = true).let {
        checkExactType<Int?>(it.g[0].info.weight[0])
        it.compareSchemas(strict = true)
    }

    df.reorderColumnsByName(inFrameColumns = false).let {
        it.compareSchemas(strict = true)
    }

    df.reorderColumnsByName(desc = true, inFrameColumns = true).let {
        it.compareSchemas(strict = true)
    }

    df.reorderColumnsByName(atAnyDepth = false, inFrameColumns = true).let {
        it.compareSchemas(strict = true)
    }

    df.reorderColumnsByName(atAnyDepth = false, desc = true, inFrameColumns = false).let {
        it.compareSchemas(strict = true)
    }
    return "OK"
}
