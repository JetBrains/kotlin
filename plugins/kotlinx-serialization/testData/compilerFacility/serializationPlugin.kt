// DUMP_IR

// MODULE: ui
// MODULE_KIND: LibraryBinary
// FILE: com/example/ui/Text.kt
package com.example.ui

fun Text(text: String) {}

// MODULE: data
// FILE: com/example/data/Data.kt
package com.example.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Data(
    var page: Int = 0,
    @SerialName(value = "name")
    val nameField: String,
    @SerialName(value = "url") val url: String,
) {

    val name: String
        get() = nameField.replaceFirstChar { it.uppercase() }

    val imageUrl: String
        inline get() {
            val index = url.split("/".toRegex()).dropLast(1).last()
            return "url://$index.png"
        }
}

// MODULE: main(data, ui)
// FILE: main.kt
package home

import com.example.data.Data
import com.example.ui.Text

fun Greeting(name: String, d: Data) {
    Text(
        text = "$name!" + d.imageUrl
    )
}
