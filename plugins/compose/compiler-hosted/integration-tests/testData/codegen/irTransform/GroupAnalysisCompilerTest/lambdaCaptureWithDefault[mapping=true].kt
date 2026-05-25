@Composable
fun Test(isEnabled: Boolean = false) {
    val composition: Composition = remember { TODO() }
    LaunchedEffect(isEnabled) {
        disposingComposition {
            composition.suspendContent { println(isEnabled) }
        }
    }
}

fun used(x: Any?) {}
