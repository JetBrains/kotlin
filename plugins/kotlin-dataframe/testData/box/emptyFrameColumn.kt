import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

interface MyEmptyDeclaration

class MyEmptyDeclarationImpl : MyEmptyDeclaration

class Test(
    val name: String,
    val containingDeclaration: List<MyEmptyDeclaration>
)

fun box(): String {
    val df = listOf(Test("Test1", listOf(MyEmptyDeclarationImpl()))).toDataFrame(maxDepth = 1)
    df.schema().print()
    val df1 = df.remove { name }
    val col: DataColumn<AnyFrame> = df1.containingDeclaration
    df1.compareSchemas(strict = true)
    return "OK"
}
