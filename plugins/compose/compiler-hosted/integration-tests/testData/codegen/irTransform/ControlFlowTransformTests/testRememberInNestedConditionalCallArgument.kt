import androidx.compose.runtime.*

@Composable
private fun Test(param: String?): String? {
    return Test(
        if (param == null) {
           Test(
                if (param == null) {
                    remember { "" }
                } else {
                    null
                }
           )
        } else {
            null
        },
    )
}
