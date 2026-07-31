// DUMP_KT_IR

// CMP-9392: an abstract @Composable function with a default parameter, declared in one module and
// overridden in another one, must keep the same parameter types in both modules. Otherwise the
// override gets a different mangled name and does not override anything, which the partial linkage
// engine reports as
// "Abstract function 'compose' is not implemented in non-abstract class 'TestValueImpl'".
//
// Note: the `main` module must not contain a call that omits `modifier`. Such a call makes
// ComposableDefaultParamLowering strip the default values off the external declaration, which
// hides the mismatch and makes this test pass on a broken compiler.

// MODULE: lib
// FILE: lib.kt
import androidx.compose.runtime.Composable

interface Modifier {
    companion object : Modifier
}

interface TestContainer {
    @Composable
    fun layout(content: @Composable () -> Unit)
}

interface TestValue {
    @Composable
    fun compose(modifier: Modifier = Modifier)

    @Composable
    fun compose(container: TestContainer) {
        container.layout {
            compose(Modifier)
        }
    }
}

// MODULE: main(lib)
// FILE: main.kt
import androidx.compose.runtime.Composable

class TestValueImpl : TestValue {
    @Composable
    override fun compose(modifier: Modifier) {
    }
}

class TestContainerImpl : TestContainer {
    @Composable
    override fun layout(content: @Composable () -> Unit) {
        content()
    }
}

@Composable
fun App() {
    TestValueImpl().compose(TestContainerImpl())
}

fun box(): String = "OK"
