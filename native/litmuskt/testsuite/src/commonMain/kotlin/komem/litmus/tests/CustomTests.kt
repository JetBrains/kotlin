package komem.litmus.tests

import komem.litmus.LitmusTest
import komem.litmus.litmusTest

val MPNoDRF: LitmusTest<*> = litmusTest({
    object {
        var x = 0
        var y = 0
        var o = 0
    }
}) {
    thread {
        x = 1
        y = 1
    }
    thread {
        o = if (y != 0) x else -1
    }
    outcome { o }
    spec {
        accept(1)
        accept(-1)
        interesting(0)
    }
}
