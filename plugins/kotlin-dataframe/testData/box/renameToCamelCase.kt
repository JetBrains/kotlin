import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    // Test DataFrame.renameToCamelCase()
    val df = dataFrameOf("first_name", "last_name", "user_age")(
        "John", "Doe", 30
    )
    val renamed = df.renameToCamelCase()

    // Verify extension properties
    if (renamed.firstName[0] != "John") return "DataFrame.renameToCamelCase: 'firstName' failed"
    if (renamed.lastName[0] != "Doe") return "DataFrame.renameToCamelCase: 'lastName' failed"
    if (renamed.userAge[0] != 30) return "DataFrame.renameToCamelCase: 'userAge' failed"

    // Test RenameClause.toCamelCase()
    val df2 = dataFrameOf("first_name", "last_name", "user_age")(
        "Jane", "Smith", 25
    )
    val renamed2 = df2.rename { first_name and last_name and user_age }.toCamelCase()

    // Verify extension properties for RenameClause.toCamelCase
    if (renamed2.firstName[0] != "Jane") return "RenameClause.toCamelCase: 'firstName' failed"
    if (renamed2.lastName[0] != "Smith") return "RenameClause.toCamelCase: 'lastName' failed"
    if (renamed2.userAge[0] != 25) return "RenameClause.toCamelCase: 'userAge' failed"

    // Test nested DataFrame with both methods
    val nestedDf = dataFrameOf("user_info")(
        dataFrameOf("first_name", "last_name")(
            "John", "Doe"
        )
    )

    // Test DataFrame.renameToCamelCase with nested
    val renamedNested = nestedDf.renameToCamelCase()
    val nestedData = renamedNested.userInfo[0]
    if (nestedData.firstName[0] != "John") return "Nested DataFrame.renameToCamelCase: 'firstName' failed"
    if (nestedData.lastName[0] != "Doe") return "Nested DataFrame.renameToCamelCase: 'lastName' failed"

    // Test RenameClause.toCamelCase with nested, only uses selection
    val renamedNested2 = nestedDf.rename { all() }.toCamelCase()
    val nestedData2 = renamedNested2.userInfo[0]
    if (nestedData2.first_name[0] != "John") return "Nested RenameClause.toCamelCase: 'first_name' failed"
    if (nestedData2.last_name[0] != "Doe") return "Nested RenameClause.toCamelCase: 'last_name' failed"

    return "OK"
}
