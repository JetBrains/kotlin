package to

import a.A
import a.ext
import a.f

fun g() {
    f {
        Inner()
        A.Nested()
        foo()
        ext()
    }
}