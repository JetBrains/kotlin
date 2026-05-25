import androidx.compose.runtime.*

var foo: @Composable () -> Unit = { Box { print("field") } }
    get() = {
        Box { 
            print("get")
        }
    }
    set(value) {
        field = {
            Box {
                print("set")
             }
        }
    }
