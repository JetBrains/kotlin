import androidx.compose.runtime.*

@Composable
fun Test(strings: List<String>) {
    val objects = strings.map { string -> 
        val stringVar = string
        object {
            val value get() = stringVar
        }
    }
    val lambda = { 
        objects.forEach { println(it.value) }
    }
}
