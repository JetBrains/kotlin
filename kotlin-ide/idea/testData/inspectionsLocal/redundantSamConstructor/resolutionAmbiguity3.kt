// PROBLEM: none

fun <T, R> with(t: T, action: T.() -> R) = t.action()

private fun usage(int1: Interfaces.InterfaceWithMethod1, int2: Interfaces.InterfaceWithMethod2) {
    with(int1) {
        with(int2) {
            foo(Interfaces.FunInterface1<caret> { }) // removing SAM constructor will change resolution to InterfaceWithMethod2::foo
        }
    }
}