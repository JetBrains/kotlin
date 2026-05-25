import androidx.compose.runtime.*

@Composable fun Example(data: Data = Data(""), intData: IntData = IntData(0)) {}
@Composable fun ExampleNullable(data: Data? = Data(""), intData: IntData = IntData(0)) {}
@Composable fun ExampleNullableData(data: NullableData = NullableData(null), intData: IntData = IntData(0)) {}
@Composable private fun PrivateExample(data: Data = Data(""), intData: IntData = IntData(0)) {}
@Composable internal fun InternalExample(data: Data = Data(""), intData: IntData = IntData(0)) {}
@Composable @PublishedApi internal fun PublishedExample(data: Data = Data(""), intData: IntData = IntData(0)) {}

abstract class Test {
    @Composable private fun PrivateExample(data: Data = Data("")) {}
    @Composable fun PublicExample(data: Data = Data("")) {}
    @Composable internal fun InternalExample(data: Data = Data("")) {}
    @Composable @PublishedApi internal fun PublishedExample(data: Data = Data("")) {}
    @Composable protected fun ProtectedExample(data: Data = Data("")) {}
}
