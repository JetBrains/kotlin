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
    val containingDeclarationDf = dataFrameOf("myName" to columnOf("abc"))
    val df = dataFrameOf(
        "name" to columnOf("Test1"),
        "containingDeclaration" to columnOf<AnyFrame>(containingDeclarationDf)
    )
    df.schema().print()
    val df1 = df.remove { name }
    val col: DataColumn<AnyFrame> = df1.containingDeclaration
    df1.compareSchemas(strict = false)
    return "OK"
}
