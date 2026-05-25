sealed class CompositionLocal2<T> {
    inline val current: T
        @Composable
        get() = error("")
    @Composable fun foo() {}
}

abstract class ProvidableCompositionLocal2<T> : CompositionLocal2<T>() {}
class DynamicProvidableCompositionLocal2<T> : ProvidableCompositionLocal2<T>() {}
class StaticProvidableCompositionLocal2<T> : ProvidableCompositionLocal2<T>() {}

fun used(x: Any?) {}
