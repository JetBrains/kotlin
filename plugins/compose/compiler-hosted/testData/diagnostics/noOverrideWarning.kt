// RUN_PIPELINE_TILL: BACKEND

// MODULE: lib
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ITest {

    @Composable
    fun test(modifier: Modifier = Modifier)
}

// MODULE: main(lib)
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class Test : ITest {

    @Composable
    override fun test(modifier: Modifier) {
    }
}