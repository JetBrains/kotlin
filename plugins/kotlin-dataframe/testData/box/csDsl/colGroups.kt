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

    compareSchemas(
        df.select { name.firstName and name.lastName },
        df.select { colsInGroups() },
        df.select { colGroups().colsInGroups() },
    )

    compareSchemas(
        dfGroup.select { name.firstName.firstName and name.firstName.secondName and name.firstName.thirdName },
        dfGroup.select { name.colsInGroups() },
        dfGroup.select { "name".colsInGroups() },
        dfGroup.select { pathOf("name").colsInGroups() },
    )

    compareSchemas(
        df.select { name },
        df.select { name() },
    )
    return "OK"
}
