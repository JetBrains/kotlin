import androidx.compose.runtime.*

val topLevelScope = @Composable { println("topLevelScope") }

@Composable
fun functionScope() {
    Box {
        print("functionScope")
    }     
}

class ClassScope {
    @Composable
    fun classScope() {
        Box {
            print("classScope")
        }
    }

    class NestedClassScope {
        @Composable
        fun nestedClassScope() {
            Box {
                print("nestedClassScope")
            }
        }
    }
}
