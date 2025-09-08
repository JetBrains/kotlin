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
    df.update { nullableB }.where { it == "myStr" }.with { "123" }.let {
        val schema = it.compileTimeSchema()
        assert(schema.column("nullableB").nullable)
    }

    // But must introduce nulls
    df.update { b }.where { it == "myStr" }.with { null }.let {
        val schema = it.compileTimeSchema()
        assert(schema.column("b").nullable)
    }

    // Sanity check: String stays non-nullable
    df.update { b }.where { it == "myStr"}.with { "123" }.let {
        val col: DataColumn<String> = it.b
    }

    df.update { b }.where { it.length == 3 }.where { it.contains("s") }.with { "123" }.let {
        val col: DataColumn<String> = it.b
    }
    return "OK"
}
