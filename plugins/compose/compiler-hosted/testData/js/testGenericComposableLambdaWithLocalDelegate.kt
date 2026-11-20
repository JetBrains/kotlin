// ISSUE: KT-89681
// DUMP_KT_IR

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlin.coroutines.EmptyCoroutineContext

@Composable
fun <T : Any> Test(fetcher: () -> T, onValue: (String) -> Unit) {
    Wrapper {
        val result: T? by remember { mutableStateOf(fetcher()) }
        onValue(result.toString())
    }
}

@Composable
fun Wrapper(content: @Composable () -> Unit) {
    content()
}

class NoOpApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}
    override fun insertBottomUp(index: Int, instance: Unit) {}
    override fun remove(index: Int, count: Int) {}
    override fun move(from: Int, to: Int, count: Int) {}
    override fun onClear() {}
}

fun box(): String {
    var result = "not composed"
    Composition(NoOpApplier(), Recomposer(EmptyCoroutineContext)).setContent {
        Test(fetcher = { "OK" }, onValue = { result = it })
    }
    return result
}
