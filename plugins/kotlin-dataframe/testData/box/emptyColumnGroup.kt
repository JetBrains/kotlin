import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.columns.*

interface MyEmptyDeclaration

class MyEmptyDeclarationImpl : MyEmptyDeclaration

class Test(
    val name: String,
    val containingDeclaration: MyEmptyDeclaration
)

fun box(): String {
    val df = listOf(Test("Test1", MyEmptyDeclarationImpl())).toDataFrame(maxDepth = 1)
    val df1 = df.remove { name }
    val group: ColumnGroup<*> = df1.containingDeclaration
    df1.compileTimeSchema().print()
    df1.schema().print()
    df1.compareSchemas(strict = true)
    return "OK"
}
