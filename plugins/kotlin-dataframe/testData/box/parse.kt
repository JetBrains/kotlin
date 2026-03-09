import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.schema.*
import kotlin.reflect.typeOf

fun box(): String {
    val nestedDf = dataFrameOf(
        "doubles" to columnOf("1.0", "2.4", null),
        "chars" to columnOf('4', '5', '6'),
        "nullableChars" to columnOf('7', null, '8'),
        "nonParse" to columnOf(7, 8, 9),
    )

    val df = dataFrameOf(
        "nonParse" to columnOf(1),
        "booleans" to columnOf("true"),
        "group" to columnOf(
            "nonParse" to columnOf(1),
            "url" to columnOf("https://example.com"),
            "long" to columnOf("1234567890"),
            "nested" to columnOf(nestedDf),
        ),
    )

    df.parse().compileTimeSchema().columns.let {
        assert(it["nonParse"]!!.type == typeOf<Int>())
        assert(it["booleans"]!!.type == typeOf<Any>())
        (it["group"] as ColumnSchema.Group).schema.columns.let {
            assert(it["nonParse"]!!.type == typeOf<Int>())
            assert(it["url"]!!.type == typeOf<Any>())
            assert(it["long"]!!.type == typeOf<Any>())
            (it["nested"] as ColumnSchema.Frame).schema.columns.let {
                assert(it["doubles"]!!.type == typeOf<Any?>())
                assert(it["chars"]!!.type == typeOf<Any>())
                assert(it["nullableChars"]!!.type == typeOf<Any?>())
                assert(it["nonParse"]!!.type == typeOf<Int>())
            }
        }
    }
    df.parse(options = ParserOptions()).compileTimeSchema().columns.let {
        assert(it["nonParse"]!!.type == typeOf<Int>())
        assert(it["booleans"]!!.type == typeOf<Any>())
        (it["group"] as ColumnSchema.Group).schema.columns.let {
            assert(it["nonParse"]!!.type == typeOf<Int>())
            assert(it["url"]!!.type == typeOf<Any>())
            assert(it["long"]!!.type == typeOf<Any>())
            (it["nested"] as ColumnSchema.Frame).schema.columns.let {
                assert(it["doubles"]!!.type == typeOf<Any?>())
                assert(it["chars"]!!.type == typeOf<Any>())
                assert(it["nullableChars"]!!.type == typeOf<Any?>())
                assert(it["nonParse"]!!.type == typeOf<Int>())
            }
        }
    }
    df.parse { valueCols() }.compileTimeSchema().columns.let {
        assert(it["nonParse"]!!.type == typeOf<Int>())
        assert(it["booleans"]!!.type == typeOf<Any>())
        (it["group"] as ColumnSchema.Group).schema.columns.let {
            assert(it["nonParse"]!!.type == typeOf<Int>())
            assert(it["url"]!!.type == typeOf<String>())
            assert(it["long"]!!.type == typeOf<String>())
            (it["nested"] as ColumnSchema.Frame).schema.columns.let {
                assert(it["doubles"]!!.type == typeOf<String?>())
                assert(it["chars"]!!.type == typeOf<Char>())
                assert(it["nullableChars"]!!.type == typeOf<Char?>())
                assert(it["nonParse"]!!.type == typeOf<Int>())
            }
        }
    }
    df.parse(ParserOptions()) { booleans and group.nested }.compileTimeSchema().columns.let {
        assert(it["nonParse"]!!.type == typeOf<Int>())
        assert(it["booleans"]!!.type == typeOf<Any>())
        (it["group"] as ColumnSchema.Group).schema.columns.let {
            assert(it["nonParse"]!!.type == typeOf<Int>())
            assert(it["url"]!!.type == typeOf<String>())
            assert(it["long"]!!.type == typeOf<String>())
            (it["nested"] as ColumnSchema.Frame).schema.columns.let {
                assert(it["doubles"]!!.type == typeOf<Any?>())
                assert(it["chars"]!!.type == typeOf<Any>())
                assert(it["nullableChars"]!!.type == typeOf<Any?>())
                assert(it["nonParse"]!!.type == typeOf<Int>())
            }
        }
    }
    df.parse("booleans", "nonParse").compileTimeSchema().columns.let {
        assert(it["nonParse"]!!.type == typeOf<Int>()) // nothing to parse
        assert(it["booleans"]!!.type == typeOf<Any>())
        (it["group"] as ColumnSchema.Group).schema.columns.let {
            assert(it["nonParse"]!!.type == typeOf<Int>())
            assert(it["url"]!!.type == typeOf<String>())
            assert(it["long"]!!.type == typeOf<String>())
            (it["nested"] as ColumnSchema.Frame).schema.columns.let {
                assert(it["doubles"]!!.type == typeOf<String?>())
                assert(it["chars"]!!.type == typeOf<Char>())
                assert(it["nullableChars"]!!.type == typeOf<Char?>())
                assert(it["nonParse"]!!.type == typeOf<Int>())
            }
        }
    }
    df.parse("group", options = ParserOptions()).compileTimeSchema().columns.let {
        assert(it["nonParse"]!!.type == typeOf<Int>())
        assert(it["booleans"]!!.type == typeOf<String>())
        (it["group"] as ColumnSchema.Group).schema.columns.let {
            assert(it["nonParse"]!!.type == typeOf<Int>())
            assert(it["url"]!!.type == typeOf<Any>())
            assert(it["long"]!!.type == typeOf<Any>())
            (it["nested"] as ColumnSchema.Frame).schema.columns.let {
                assert(it["doubles"]!!.type == typeOf<Any?>())
                assert(it["chars"]!!.type == typeOf<Any>())
                assert(it["nullableChars"]!!.type == typeOf<Any?>())
                assert(it["nonParse"]!!.type == typeOf<Int>())
            }
        }
    }
    return "OK"
}
