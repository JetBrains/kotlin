import androidx.compose.runtime.*

@Composable
fun Test() {
    try {
        try {
            if (true) {
                foo(block = {})
            }
        } catch (ignored: Exception) {
            if (true) {
                foo(block = {})
            }
        } finally {
            if (true) {
                foo(block = {})
            }
        }

        if (true) {
            foo(block = {})
        }
    } catch (ignored: Exception) {
        if (true) {
            foo(block = {})
        }
    } finally {
        if (true) {
            foo(block = {})
        }
    }

    if (true) {
        foo(block = {})
    }
}
