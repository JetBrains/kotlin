import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val json = """[{"a":1,"b":2,"c":3,"group":{"x":10}}]"""
    val df = DataFrame.readJsonStr(json)

    df.move { "a"() }.under("newGroup").let {
        val v: Any? = it[0].newGroup.a
        it.compareSchemas()
    }

    df.move { "a"() }.under { "group"() }.let {
        val v: Any? = it[0].group.a
        it.compareSchemas()
    }

    df.move { "a"() }.into("renamed").let {
        val v: Any? = it[0].renamed
        it.compareSchemas()
    }

    df.move { "c"() }.toStart().let {
        val v: Any? = it[0].c
        it.compareSchemas()
    }

    df.moveToStart { "c"() }.let {
        val v: Any? = it[0].c
        it.compareSchemas()
    }

    df.move { "a"() }.toEnd().let {
        val v: Any? = it[0].a
        it.compareSchemas()
    }

    df.moveToEnd { "a"() }.let {
        val v: Any? = it[0].a
        it.compareSchemas()
    }

    df.move { "c"() }.to(0).let {
        val v: Any? = it[0].c
        it.compareSchemas()
    }

    df.moveTo(0) { "c"() }.let {
        val v: Any? = it[0].c
        it.compareSchemas()
    }

    return "OK"
}