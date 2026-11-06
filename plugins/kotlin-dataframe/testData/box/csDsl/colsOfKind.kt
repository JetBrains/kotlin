import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { age and city and weight and isHappy },
        df.select { colsOfKind(ColumnKind.Value) },
        df.select { all().colsOfKind(ColumnKind.Value) },
    )

    compareSchemas(
        df.select { name },
        df.select { colsOfKind(ColumnKind.Group) },
        df.select { all().colsOfKind(ColumnKind.Group) },
    )

    compareSchemas(
        df.select { all() },
        df.select { colsOfKind(ColumnKind.Value, ColumnKind.Group) },
    )

    compareSchemas(
        df.select { name.firstName and name.lastName },
        df.select { name.colsOfKind(ColumnKind.Value) },
        df.select { "name".colsOfKind(ColumnKind.Value) },
        df.select { pathOf("name").colsOfKind(ColumnKind.Value) },
    )

    val dfFrames = dataFrameOf(
        "a" to columnOf(1),
        "frameCol" to columnOf(dataFrameOf("b" to columnOf(2))),
    )
    compareSchemas(
        dfFrames.select { frameCol },
        dfFrames.select { colsOfKind(ColumnKind.Frame) },
    )
    return "OK"
}
