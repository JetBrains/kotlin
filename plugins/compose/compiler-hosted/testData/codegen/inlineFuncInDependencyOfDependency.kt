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
    inline fun giveMeString() : String {
        return secret()
    }

    @PublishedApi
    internal fun secret() : String {
        return "what is up!!!!!!!"
    }
}

// MODULE: moduleWithoutInline(myModule)
// FILE: com/example/moduleWithoutInline/Foo.kt
package com.example.moduleWithoutInline

import com.example.myModule.OtherModule

fun foo(name: String) : String = "$name!" + OtherModule().giveMeString()

// MODULE: main(moduleWithoutInline, ui)
// FILE: main.kt
package home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.moduleWithoutInline.foo
import com.example.ui.Text

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = foo(name),
        modifier = modifier
    )
}
