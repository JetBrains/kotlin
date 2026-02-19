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
    df.update { nullableB }.at(listOf(1)).with { "123" }.let {
        val schema = it.compileTimeSchema()
        assert(schema.column("nullableB").nullable)
    }

    df.update { nullableB }.at(1).with { "123" }.let {
        val schema = it.compileTimeSchema()
        assert(schema.column("nullableB").nullable)
    }

    df.update { nullableB }.at(1..1).with { "123" }.let {
        val schema = it.compileTimeSchema()
        assert(schema.column("nullableB").nullable)
    }

    // But must introduce nulls
    df.update { b }.at(listOf(1)).with { null }.let {
        val schema = it.compileTimeSchema()
        assert(schema.column("b").nullable)
    }

    // Sanity check: String stays non-nullable
    df.update { b }.at(listOf(1)).with { "123" }.let {
        val col: DataColumn<String> = it.b
    }

    df.update { b }.at(0, 1).where { it.contains("s") }.with { "123" }.let {
        val col: DataColumn<String> = it.b
    }

    return "OK"
}
