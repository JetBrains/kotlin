import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "nullableB" to columnOf(null, "str"),
        "b" to columnOf("bbb", "str"),
    )

    // No longer should remove nulls
    df.fillNulls { nullableB }.where { b == "bbb" }.with { "123" }.let {
        val schema = it.compileTimeSchema()
        assert(schema.column("nullableB").nullable)
    }

    // fillNulls cannot introduce nulls
    df.fillNulls { b }.where { b == "bbb" }.with { null }.let {
        val schema = it.compileTimeSchema()
        val col: DataColumn<String> = it.b
    }

    // Sanity check: String stays non-nullable
    df.fillNulls { b }.where { b == "bbb" }.with { "123" }.let {
        val col: DataColumn<String> = it.b
    }
    return "OK"
}
