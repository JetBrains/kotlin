import androidx.compose.runtime.*

@Composable
private fun Test(param: String?) {
    Dialog {
        if (false) Test(param)
    }
}
