// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Store (
    val storeID: Int,
    val storeRegion: String,
    val revenue: Long,
    val expenses: Long
)

fun box(): String {
    val df = dataFrameOf(
        "group" to columnOf(
            dataFrameOf(
                Store(23, "Chicago", 500, 400),
                Store(71, "Chicago", 600, 480),
            ),
            dataFrameOf(
                Store(23, "Chicago", 500, 400),
                Store(71, "Chicago", 600, 480),
            ),
            null
        )
    )

    checkExactType<DataFrame<Store>?>(
        df.group[0]
    )

    val updated = df.add("dummyCol") { 123 }
    checkExactType<DataFrame<Store>?>(
        updated.group[0]
    )

    println(updated.group.kind())

    return "OK"
}
