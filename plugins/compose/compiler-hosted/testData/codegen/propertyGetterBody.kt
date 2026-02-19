// DUMP_IR

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: compose/ui/foo.kt
package compose.ui

import androidx.compose.runtime.Composable

class Color(val r: Int, val g: Int, val b:Int)

interface ColumnScope {
}

@Composable
inline fun Column(content: @Composable ColumnScope.() -> Unit) {
}

// MODULE: main(lib)
// FILE: TextFieldState.kt
package home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

open class TextFieldState {
    var text: String by mutableStateOf("")
}

// FILE: EmailState.kt
package home

class EmailState(val email: String? = null) : TextFieldState() {
    init {
        email?.let {
            text = it
        }
    }
}

// FILE: main.kt
package home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import compose.ui.Column

@Composable
fun Home() {
    val emailState by remember { mutableStateOf(EmailState()) }
    Column {
        emailState.text
    }
}
