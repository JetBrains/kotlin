// DUMP_IR

// MODULE: main
// FILE: main.kt
package home

import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun ReplyNavHost(
    inbox: String,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tooltipState = remember { TooltipState() }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip {
                Text("Tooltip")
            }
        },
        state = tooltipState
    ) {
        scope.launch { tooltipState.show() }
    }
}

// If `StubBasedFirDeserializedSymbolProvider::findAndDeserializeClass` sets `symbol.fir.isNewPlaceForBodyGeneration` correctly,
// the generated class file must have no `androidx/compose/material3/TooltipState$DefaultImpls`, because
// `androidx/compose/material3/TooltipState` class file header does not have "default impls for interface" JVM flag.