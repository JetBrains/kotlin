// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun Wrapper(content: @Composable () -> Unit) {
    content()
}

@Composable
fun WI(content: @Composable () -> Unit) {
    Wrapper(content)
}

@Composable
fun ANW() {
  Wrapper {
      BN()
  }
}

@Composable
fun ANW_I() {
  WI {
    BN()
  }
}

@Composable
fun BN() {
   N()
}

@Composable
@ComposableTarget("N")
fun N() {}

@Composable
fun AMW() {
  Wrapper {
      BM()
  }
}

@Composable
fun AMW_I() {
  WI {
    BM()
  }
}

@Composable
fun BM() {
   M()
}

@Composable
@ComposableTarget("M")
fun M() {}
