import androidx.compose.runtime.*

@Composable
fun BrokenComposable(
    composableParameter: @Composable (
        parameter1: String,
        parameter2: String,
        parameter3: String,
        parameter4: String,
        parameter5: String,
        parameter6: String,
        parameter7: String,
        parameter8: String,
        parameter9: String,
        parameter10: String
    ) -> Unit
) {
    val composableWithData = @Composable {
        composableParameter(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        )
    }
}
