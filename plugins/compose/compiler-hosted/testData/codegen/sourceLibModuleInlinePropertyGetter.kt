// DUMP_IR

// MODULE: ui
// MODULE_KIND: LibraryBinary
// FILE: com/example/ui/Text.kt
package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Text(text: String, modifier: Modifier) {}

// MODULE: myModule
// FILE: com/example/myModule/OtherModule.kt
package com.example.myModule

class OtherModule {
    val message: String
        inline get() {
            return secret()
        }

    @PublishedApi
    internal fun secret() : String {
        return "what is up!!!!!!!"
    }
}

// MODULE: main(myModule, ui)
// FILE: main.kt
package home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myModule.OtherModule
import com.example.ui.Text

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "$name!" + OtherModule().message,
        modifier = modifier
    )
}
