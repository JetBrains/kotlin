// ISSUE: KT-69806

import Outer.AllOpen

@AllOpen
class Outer {
    annotation class AllOpen
}
