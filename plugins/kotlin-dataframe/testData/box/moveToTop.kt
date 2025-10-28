import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b")(1, 2).group { a and b }.into("c").move { c.a }.toTop()
    df.a
    df.c.b
    df.checkCompileTimeSchemaEqualsRuntime()

    val df2 = dataFrameOf("a", "b")(1, 2).group { a and b }.into("c").move { c.a }.toTop { "newName" }
    require(df2.compileTimeSchema().columns.isEmpty()) { "move toTop with lambda did not result in an empty compile time schema." }
    return "OK"
}
