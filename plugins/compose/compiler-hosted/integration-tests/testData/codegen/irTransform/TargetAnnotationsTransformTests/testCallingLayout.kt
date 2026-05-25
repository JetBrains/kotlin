import androidx.compose.runtime.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.*
import androidx.compose.foundation.text.*

@Composable
fun Test1() {
    Layout(content = { }) { _, _ -> error("") }
}

@Composable
fun Test2(content: @Composable ()->Unit) {
    Layout(content = content) { _, _ -> error("") }
}

@Composable
fun Test3() {
  Test1()
}

@Composable
fun Test4() {
  BasicText(text = AnnotatedString("Some text"))
}

val Local = compositionLocalOf { 0 }

@Composable
fun Test5(content: @Composable () -> Unit) {
  CompositionLocalProvider(Local provides 5) {
      Test1()
      content()
  }
}

@Composable
fun Test6(test: String) {
  CompositionLocalProvider(Local provides 6) {
     T(test)
     Test1()
  }
}

@Composable
fun T(value: String) { }
