// A composable singleton lambda is stored in ComposableSingletons, where type parameters declared outside
// the lambda are out of scope, so it gets its own copies of the ones it references, wherever they are declared.

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlin.coroutines.EmptyCoroutineContext

val log = mutableListOf<String>()

class Holder<T> {
    @Composable
    fun ClassTypeParameter() {
        Consume<T> { log += "class" }
    }

    @Composable
    fun <S> ClassAndFunctionTypeParameters() {
        Consume<Pair<S, T>> { log += "class and function" }
    }
}

@Composable
fun <T> NestedLambda() {
    Wrapper {
        Consume<T> { log += "nested lambda" }
    }
}

@Composable
fun <T> LocalFunction() {
    @Composable
    fun Local() {
        Consume<T> { log += "local function" }
    }
    Local()
}

fun <T> nonComposable(): @Composable () -> Unit = {
    Consume<T> { log += "non-composable function" }
}

@Composable
fun <A, B : List<A>> TypeParameterBound() {
    Consume<B> { log += "bound" }
}

@Composable
fun <S> Consume(content: @Composable (List<S>) -> Unit) {
    content(emptyList())
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
    val nonComposableContent = nonComposable<String>()
    Composition(NoOpApplier(), Recomposer(EmptyCoroutineContext)).setContent {
        Holder<String>().ClassTypeParameter()
        Holder<String>().ClassAndFunctionTypeParameters<Int>()
        NestedLambda<String>()
        LocalFunction<String>()
        nonComposableContent()
        TypeParameterBound<String, List<String>>()
    }
    val expected = listOf("class", "class and function", "nested lambda", "local function", "non-composable function", "bound")
    return if (log == expected) "OK" else "Fail: $log"
}
