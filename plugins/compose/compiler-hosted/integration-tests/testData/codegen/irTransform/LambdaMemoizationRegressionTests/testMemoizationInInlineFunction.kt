import androidx.compose.runtime.*

@Composable
inline fun Test(
    someBool: Boolean,
) {
    val someInt = remember { 1 }
    val lambda = { someInt }
    println(lambda.hashCode())
}
