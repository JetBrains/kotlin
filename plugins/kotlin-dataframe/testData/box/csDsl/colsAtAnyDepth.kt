import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df1 = df.select { name }

    compareSchemas(
        df1.select { name.firstName and name.lastName },
        df1.select { name.colsAtAnyDepth() },
    )

    compareSchemas(
        df1.select { name and name.firstName and name.lastName },
        df1.select { colsAtAnyDepth() },
        df1.select { nameStartsWith("name").colsAtAnyDepth() },
    )
    return "OK"
}
