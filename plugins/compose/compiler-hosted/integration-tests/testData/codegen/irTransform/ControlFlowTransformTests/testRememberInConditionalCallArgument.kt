import androidx.compose.runtime.*

@Composable
private fun Test(param: String?) {
    Test(
        if (param == null) {
           remember { "" }
        } else {
            null
        },
    )
}
