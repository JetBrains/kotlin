import androidx.compose.runtime.*

@Composable
fun TwoLambdas(
    lambda1: () -> Unit,
    lambda2: (Int) -> Unit
) {
    lambda1()
    lambda2(0)
}
