import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
<!DATA_SCHEMA_DECLARATION_VISIBILITY!>private<!> interface A {
    val a: Int
}

@DataSchema
internal interface B {
    val a: Int
}

@DataSchema
public interface C {
    val a: Int
}

@DataSchema
interface D {
    val a: Int
}

open class Container {
    @DataSchema
    <!DATA_SCHEMA_DECLARATION_VISIBILITY!>protected<!> interface E
}