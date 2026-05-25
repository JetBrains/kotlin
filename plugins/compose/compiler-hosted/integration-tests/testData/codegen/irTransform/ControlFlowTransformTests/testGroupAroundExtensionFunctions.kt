import androidx.compose.runtime.*

@Composable
fun Test(start: Int, end: Int) {
    val a = remember { A() }
    for (i in start until end) {
        val b = a.get(bKey)
        if (i == 2) {
            a.get(cKey)
        }
    }
}
