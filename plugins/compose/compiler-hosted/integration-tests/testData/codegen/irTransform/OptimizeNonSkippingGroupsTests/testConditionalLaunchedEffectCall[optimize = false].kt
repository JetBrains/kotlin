import androidx.compose.runtime.*

@Composable
fun Test(states: List<String>, condition: Boolean) {
    states.forEach { state ->
        key(state) {
            if (condition) {
                LaunchedEffect(state) { println(state) }
            }
            TwoLambdas(
                lambda1 = { println(state) },
                lambda2 = { println(state) }
            )
        }
    }
}
