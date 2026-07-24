import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    // region ColumnSet except

    df.select { colsAtAnyDepth().colsOf<String?>() except { name.firstName and cols(city) } }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { colsAtAnyDepth().colsOf<String?>() except (name.firstName and cols(city)) }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { colsAtAnyDepth().colsOf<String?>().except(name.firstName, cols(city)) }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { colsAtAnyDepth().colsOf<String?>() except "city" }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { colsAtAnyDepth().colsOf<String?>().except("city", "name") }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { colsAtAnyDepth().colsOf<String?>() except pathOf("city") }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { colsAtAnyDepth().colsOf<String?>().except(pathOf("city"), "name"["firstName"]) }
        .checkCompileTimeSchemaEqualsRuntime()

    // endregion

    // region ColumnsSelectionDsl allExcept

    df.select { allExcept { name.firstName and cols(city) } }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { allExcept(name.firstName, cols(city)) }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { allExcept("name", "city") }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { allExcept(pathOf("city"), "name"["firstName"]) }
        .checkCompileTimeSchemaEqualsRuntime()

    // endregion

    // region SingleColumn allColsExcept

    df.select { name.allColsExcept { firstName } }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { name.allColsExcept("firstName") }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { name.allColsExcept(pathOf("firstName")) }
        .checkCompileTimeSchemaEqualsRuntime()

    // endregion

    // region String allColsExcept

    df.select { "name".allColsExcept { "firstName"() } }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { "name".allColsExcept("firstName") }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { "name".allColsExcept(pathOf("firstName")) }
        .checkCompileTimeSchemaEqualsRuntime()

    // endregion

    // region ColumnPath allColsExcept

    df.select { pathOf("name").allColsExcept { "firstName"() } }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { pathOf("name").allColsExcept("firstName") }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { pathOf("name").allColsExcept(pathOf("firstName")) }
        .checkCompileTimeSchemaEqualsRuntime()

    // endregion
    
    // region SingleColumn except

    df.select { name.except { firstName } }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { name.except("firstName") }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { name.except(pathOf("firstName")) }
        .checkCompileTimeSchemaEqualsRuntime()

    // endregion

    // region String except

    df.select { "name".except { "firstName"() } }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { "name".except("firstName") }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { "name".except(pathOf("firstName")) }
        .checkCompileTimeSchemaEqualsRuntime()

    // endregion

    // region ColumnPath except

    df.select { pathOf("name").except { "firstName"() } }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { pathOf("name").except("firstName") }
        .checkCompileTimeSchemaEqualsRuntime()

    df.select { pathOf("name").except(pathOf("firstName")) }
        .checkCompileTimeSchemaEqualsRuntime()

    // endregion

    return "OK"
}
