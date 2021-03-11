package stopInLambdaInInlinedCallWithCrossInline

// KT-15282

fun main(args: Array<String>) {
    foo {
        //Breakpoint!
        12
    }
}

fun bar(f: () -> Unit) {
    f()
}

inline fun foo(crossinline func: () -> Int) {
    bar {
        func()
    }
}