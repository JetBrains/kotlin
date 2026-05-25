import androidx.compose.runtime.*

class A

class B

class C

val bKey: () -> B = { B() }
val cKey: () -> C = { C() }

@Composable
fun <T> A.get(block: () -> T) = block()
