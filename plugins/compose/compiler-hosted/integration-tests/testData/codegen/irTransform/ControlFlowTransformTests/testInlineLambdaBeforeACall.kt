import androidx.compose.runtime.*

@Composable
private fun Test(param: String?): String? {
    InlineNonComposable {
        repeat(10) {
            Test("InsideInline")
        }
    }
    return Test("AfterInline")
}
