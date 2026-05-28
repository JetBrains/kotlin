// DUMP_IR

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: SettingsCard.kt
package test

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface SettingsCard {
    val cardTitle: String

    @Composable
    fun CardContents(modifier: Modifier = Modifier) { }
}

// MODULE: main(lib)
package home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import test.SettingsCard

class Card(override val cardTitle: String) : SettingsCard {
    @Composable
    override fun CardContents(modifier: Modifier) {
        Box(modifier)
    }
}

@Composable fun Test(card: SettingsCard) {
    card.CardContents()
}
