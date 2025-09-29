import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a" to columnOf(1, null), "b" to columnOf(null, ""))
    // notNull { } operation keep nullability as is, and in many cases it's redundant
    val df1 = df.update { b }.notNull { "empty" }
    val schema = df1.compileTimeSchema()
    require(schema.column("b").nullable)
    df1.compareSchemas(strict = true)

    // Nothing at all happens
    val df2 = df.fillNulls { a }.notNull { 123 }
    df2.compareSchemas(strict = true)

    val df3 = df.update { a and b }.notNull { it }
    df3.compareSchemas(strict = true)

    val df4 = df.update { b }.notNull().with { "empty" }
    df4.compareSchemas(strict = true)
    return "OK"
}
