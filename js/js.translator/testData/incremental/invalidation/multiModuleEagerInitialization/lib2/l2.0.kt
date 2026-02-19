//@OptIn(kotlin.ExperimentalStdlibApi::class)
//@EagerInitialization
val x = run { z1 = true; 42 }

//@OptIn(kotlin.ExperimentalStdlibApi::class)
//@EagerInitialization
val y = run { z2 = true; 117 }
