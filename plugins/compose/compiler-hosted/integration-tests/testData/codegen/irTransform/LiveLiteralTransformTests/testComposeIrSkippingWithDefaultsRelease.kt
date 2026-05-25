import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions

object Ui {}

@Composable
fun Ui.UiTextField(
    isError: Boolean = false,
    keyboardActions2: Boolean = false,
) {
    println("t41 insideFunction $isError")
    println("t41 insideFunction $keyboardActions2")
    Column {
        Text("$isError")
        Text("$keyboardActions2")
    }
}
