import androidx.compose.runtime.*

@Composable
fun root() {
    Box {
        print("root/1") 
        Box {
            print("root/1/1")
        }
        
        Box {
            print("root/1/2")

            Box {
                print("root/1/2/1")
            }
            
            Box {
                print("root/1/2/2")
            }
        }
    }
}
