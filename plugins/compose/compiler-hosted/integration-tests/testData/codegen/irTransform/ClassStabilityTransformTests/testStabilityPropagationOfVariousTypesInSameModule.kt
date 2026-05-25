import a.*
import androidx.compose.runtime.Composable

class X<T>(val p1: List<T>)
class StableDelegateProp {
    var p1 by StableDelegate()
}
class UnstableDelegateProp {
    var p1 by UnstableDelegate()
}
@Composable fun A(y: Any) {
    used(y)
    A(X(listOf(StableClass())))
    A(StableDelegateProp())
    A(UnstableDelegateProp())
    A(SingleParamProp(0))
    A(SingleParamNonProp(0))
    A(SingleParamProp(Any()))
}
