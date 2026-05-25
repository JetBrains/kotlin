import androidx.compose.runtime.*

@Composable fun Test(test: Boolean) {
    Column {
       if (!test) {
           Text("Say")
           return@Column
       }
       Text("Hello")
    }

    NonComposable {
        if (!test) {
           Text("Say")
           return@NonComposable
       }
       Text("Hello")
    }
}
