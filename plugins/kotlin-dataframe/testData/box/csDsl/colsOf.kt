import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { city },
        df.select { colsOf<String?>() },
        df.select { all().colsOf<String?>() },
    )

    compareSchemas(
        df.select { name.firstName and name.lastName },
        df.select { name.colsOf<String>() },
    )

    compareSchemas(
        df.select { name },
        df.select { colsOf<AnyRow>() }
    )

    val df1 = dataFrameOf("nestedDf")(dataFrameOf("a", "b")(1, 2))
    compareSchemas(
        df1.select { nestedDf },
        df1.select { colsOf<AnyFrame>() }
    )
    return "OK"
}
