import androidx.compose.runtime.*

@Composable
fun Box(child: @Composable () -> Unit) {
    print("box")
    child()
}

@Composable
inline fun Foo(crossinline child: @Composable () -> Unit) {
    val a = @Composable {
        print("a")
    }
    
    Box {
         print("foo")
         a()
         child()
    }
}

@Composable
fun Test() {
    Foo {
        print("test")
    }           
}
