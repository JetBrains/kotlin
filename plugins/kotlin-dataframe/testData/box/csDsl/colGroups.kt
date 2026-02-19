import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { name },
        df.select { colGroups() },
        df.select { nameContains("e").colGroups() },
    )

    compareSchemas(
        dfGroup.select { name.firstName },
        dfGroup.select { name.colsNameContains("Name").colGroups() },
    )
    return "OK"
}
