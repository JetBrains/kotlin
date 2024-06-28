package lib

inline fun anInlineFunction(crossinline crossInlineLamba: () -> Unit) {
    Foo().apply {
        barMethod { crossInlineLamba() }
    }
}

class Foo {
    fun barMethod(aLambda: () -> Unit) { aLambda() }
}
