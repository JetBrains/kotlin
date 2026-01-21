import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = DataFrame.readJsonStr("{\"a\":1, \"b\":2}")

    df.split { "a"() }.by { listOf(1, 2) }.into("c", "d").let {
        val v: Int? = it[0].c
        it.compareSchemas()
    }
    df.update { "a"<Int>() }.with { 1 }.let {
        val v: Int = it[0].a
        it.compareSchemas()
    }

    df.implode { "a"() }.let {
        val v: List<Any?> = it[0].a
        it.compareSchemas()
    }

    df.rename { "b"() }.into("c").let {
        val v: Any? = it[0].c
        it.compareSchemas()
    }

    DataFrame.readJsonStr("{\"b_B\":2}").rename { "b_B"() }.toCamelCase().let {
        val v: Any? = it[0].bB
        it.compareSchemas()
    }

    df.group { "a"() and "b"() }.into("c").let {
        val v: Any? = it[0].c.a
        val v1: Any? = it[0].c.b
        it.compareSchemas()
    }

    df.dropNulls { "a"() }.let {
        val v: Any = it[0].a
        it.compareSchemas()
    }

    df.dropNA { "a"() }.let {
        val v: Any = it[0].a
        it.compareSchemas()
    }

    DataFrame.readJsonStr("[{\"a\":1}, {\"a\":null}]").fillNulls { "a"<Int?>() }.with { 0 }.let {
        val v: Int = it[0].a
        it.compareSchemas(strict = true)
    }

    DataFrame.readJsonStr("{\"a\":[1], \"b\":2}").explode { "a"<List<Int>>() }.let {
        val v: Int = it[0].a
        it.compareSchemas()
    }

    df.gather { "a"() and "b"() }.into("key", "ab").let {
        val v: Any? = it[0].ab
        it.assert()
    }

    df.convert { "a"() }.asColumn { it }.let {
        val v: Any? = it[0].a
        it.compareSchemas()
    }

    class Record(val f: String)

    val unfoldDf: AnyFrame = dataFrameOf("record" to columnOf(Record("1")))

    unfoldDf.unfold { "record"<Record>() }.let {
        val v: String = it[0].record.f
        it.compareSchemas()
    }

    df.rename("a" to "newA", "b" to "newB").let {
        val v1: Any? = it[0].newA
        val v2: Any? = it[0].newB
        it.compareSchemas()
    }

    df.merge { "a"() and "b"() }.into("c").let {
        val v1: List<Any?> = it[0].c
        it.compareSchemas()
    }

    df.reorder { "a"() and "b"() }.byName(desc = true).let {
        val v1: Any? = it[0].b
        val v2: Any? = it[0].a
        it.compareSchemas()
    }

    df.groupBy { "a"() }.aggregate { a.first() into "v" }.let {
        val v1: Any? = it[0].a
        val v2: Any? = it[0].v
        it.compareSchemas()
    }

    return "OK"
}
