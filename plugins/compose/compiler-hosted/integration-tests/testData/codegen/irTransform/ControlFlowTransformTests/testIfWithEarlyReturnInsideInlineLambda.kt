import androidx.compose.runtime.Composable

@Composable fun Test() {
    run {
        if (true) {
            return@run
        } else {
            Test()
            return@run
        }
    }
}
