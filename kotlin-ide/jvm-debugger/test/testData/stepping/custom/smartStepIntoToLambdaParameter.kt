package smartStepIntoToLambdaParameter

fun nonDefaultParameter(paramFun: (Int) -> Int) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    paramFun(1)
}

fun defaultParameter(f: (String) -> Int = { it.toInt() }) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    f("12")
}

// Minor bug is here. During selection paramFun() is shown as a target, but f is called instead.
fun firstInvokeIsCalled(f: (String) -> Int = { it.toInt() }, paramFun: (Int) -> Int) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    paramFun(f("12")) + ii()
}

fun nonDefaultWithAnonymousFun(paramFun: (Int) -> Int) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    paramFun(12)
}

fun withExtensionParameters(paramFun: Int.() -> Int) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    12.paramFun()
}

fun <T> genericSignatureAndExtensionArgument(v: T, paramFun: (T) -> T) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    paramFun(v)
}

fun main(args: Array<String>) {
    val localFun: (Int) -> Int = { it + 1 }

    nonDefaultParameter(localFun)

    defaultParameter()

    firstInvokeIsCalled { it + 1 }

    nonDefaultWithAnonymousFun(fun (i: Int): Int { return i })

    withExtensionParameters { this }

    val ext: Int.() -> Int = { this }
    genericSignatureAndExtensionArgument(15, ext)
}

fun ii() = 12