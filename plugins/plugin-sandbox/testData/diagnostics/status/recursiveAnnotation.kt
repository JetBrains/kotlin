// ISSUE: KT-69806

import Outer.AllOpen

annotation class Some

@AllOpen
class Outer {
    @Some
    annotation class AllOpen
}

// ----------------------------------------------

@Outer2.AllOpen2
class Outer1 {
    @Some
    annotation class AllOpen
}

@Outer1.AllOpen
class Outer2 {
    @Some
    annotation class AllOpen2
}
