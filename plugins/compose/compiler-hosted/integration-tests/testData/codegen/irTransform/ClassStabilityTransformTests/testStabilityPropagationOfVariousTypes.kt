import a.*
import androidx.compose.runtime.Composable

@Composable fun A(y: Any? = null) {
    used(y)
    A()
    A(EmptyClass())
    A(SingleStableValInt(123))
    A(SingleStableVal(StableClass()))
    A(SingleParamProp(StableClass()))
    A(SingleParamProp(UnstableClass()))
    A(SingleParamNonProp(StableClass()))
    A(SingleParamNonProp(UnstableClass()))
    A(DoubleParamSingleProp(StableClass(), StableClass()))
    A(DoubleParamSingleProp(UnstableClass(), StableClass()))
    A(DoubleParamSingleProp(StableClass(), UnstableClass()))
    A(DoubleParamSingleProp(UnstableClass(), UnstableClass()))
    A(X(listOf(StableClass())))
    A(X(listOf(StableClass())))
    A(NonBackingFieldUnstableVal())
    A(NonBackingFieldUnstableVar())
    A(StableDelegateProp())
    A(UnstableDelegateProp())
}
