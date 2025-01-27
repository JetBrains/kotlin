import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    // Create DataFrame with snake_case column names
    val df = dataFrameOf("first_name", "last_name", "user_age")(
        "John", "Doe", 30
    )

    // Rename columns to camelCase
    val renamed = df.renameToCamelCase()

    // Access columns through generated extension properties
    if (renamed.firstName[0] != "John") return "Extension property 'firstName' failed"
    if (renamed.lastName[0] != "Doe") return "Extension property 'lastName' failed"
    if (renamed.userAge[0] != 30) return "Extension property 'userAge' failed"

    // Test nested DataFrame renaming
    val nestedDf = dataFrameOf("user_info")(
        dataFrameOf("first_name", "last_name")(
            "John", "Doe"
        )
    )
    val renamedNested = nestedDf.renameToCamelCase()

    // Access nested columns through generated extension properties
    val nestedData = renamedNested.userInfo[0]
    if (nestedData.firstName[0] != "John") return "Extension property 'firstName' failed"
    if (nestedData.lastName[0] != "Doe") return "Extension property 'lastName' failed"

    return "OK"
}
