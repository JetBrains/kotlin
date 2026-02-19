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

@JvmInline
value class Password(private val s: String)

// MODULE: main(myModule, ui)
// FILE: main.kt
package home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myModule.Password
import com.example.ui.Text

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val securePassword = Password("Don't try this in production: $name")
    Text(
        text = "text: $securePassword",
        modifier = modifier
    )
}
