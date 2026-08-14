// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true -Xdisable-phases=OptimizeCasts
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

// KT-88544: ComputeTypesPass used to take the variable values reaching a loop's condition and a
// loop's exit from the fall-through path only, discarding the values coming from `continue` and
// `break`. A write performed on such a path stayed invisible, so the variable was narrowed down
// to the remaining writes only, and reading it produced an unsafe downcast to a final class.
// This is a different problem than KT-86949, which is about a merge point's type becoming final
// during the analysis. OptimizeCasts is disabled so that ComputeTypesPass is tested in isolation.

sealed interface State {
    data object Initial : State

    data class Updated(val value: Int) : State
}

fun proceed() = true

// The write to `state` reaches the code after the loop only via `break`.
fun reachedByBreak(): Int {
    var state: State = State.Initial
    while (proceed()) {
        state = State.Updated(42)
        break
    }
    return when (val current = state) {
        State.Initial -> 0
        is State.Updated -> current.value
    }
}

// The write to `state` reaches the next iteration only via `continue`, and the value is read there
// by an exhaustive `when` over a subject variable.
fun reachedByContinue(): Int {
    var state: State = State.Initial

    for (shouldUpdate in listOf(true, false)) {
        if (shouldUpdate) {
            state = State.Updated(42)
            if (state is State.Updated) {
                state.value
            }
            continue
        }

        return when (val current = state) {
            State.Initial -> 0
            is State.Updated -> current.value
        }
    }

    error("unreachable")
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    val byBreak = reachedByBreak()
    if (byBreak != 42) return "fail: break variant returned $byBreak"

    val byContinue = reachedByContinue()
    if (byContinue != 42) return "fail: continue variant returned $byContinue"

    return "OK"
}
