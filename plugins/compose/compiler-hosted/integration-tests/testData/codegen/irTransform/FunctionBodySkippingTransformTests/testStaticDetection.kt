import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


val normInt = 345
val stableTopLevelProp: Modifier = Modifier

// all of these should result in 0b0110
@Composable fun A() {
    val x = 123
    D {}
    C({})
    C(stableFun(123))
    C(16.dp + 10.dp)
    C(Dp(16))
    C(16.dp)
    C(normInt)
    C(Int.MAX_VALUE)
    C(stableTopLevelProp)
    C(Modifier)
    C(Foo.Bar)
    C(constInt)
    C(123)
    C(123 + 345)
    C(x)
    C(x * 123)
}
// all of these should result in 0b0000
@Composable fun B() {
    C(Math.random())
    C(Math.random() / 100f)
}
