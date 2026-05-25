import androidx.compose.runtime.*            

@Composable
fun LoginInputFields(
    loginError: Int?,
) {
    val text = loginError?.let { stringResource(resource = it) }.orEmpty()

    Checkbox(
        checked = false,
        onChecked = {},
    )
}
