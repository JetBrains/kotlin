/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

struct RunsInfo {
    static let RUNS = 2_000_000
}

protocol I0 {}
protocol I1 {}
protocol I2: I0 {}
protocol I3: I1 {}
protocol I4: I0, I2 {}
protocol I5: I3 {}
protocol I6: I0, I2, I4 {}
protocol I9: I0, I2, I4 {}
protocol I7: I1 {}
protocol I8: I0, I1 {}

class CastsBenchmark {
    class C0: I0 {}
    class C1: C0, I1 {}
    class C2: C1, I2 {}
    class C3: C2, I3 {}
    class C4: C3, I4 {}
    class C5: C4, I5 {}
    class C6: C5, I6 {}
    class C9: C5, I9 {}
    class C7: C3, I7 {}
    class C8: C3, I8 {}

    private func foo_class(_ c: Any, _ x: Int, _ i: Int) -> Int {
        var x = x
        if (c is C0) {
            x = x &+ i
        }
        if (c is C1) {
            x = x ^ i
        }
        if (c is C2) {
            x = x &+ i
        }
        if (c is C3) {
            x = x ^ i
        }
        if (c is C4) {
            x = x &+ i
        }
        if (c is C5) {
            x = x ^ i
        }
        if (c is C6) {
            x = x &+ i
        }
        if (c is C7) {
            x = x ^ i
        }
        if (c is C8) {
            x = x &+ i
        }
        if (c is C9) {
            x = x ^ i
        }
        return x
    }

    private func foo_iface(_ c: Any, _ x: Int, _ i: Int) -> Int {
        var x = x
        if (c is I0) {
            x = x &+ i
        }
        if (c is I1) {
            x = x ^ i
        }
        if (c is I2) {
            x = x &+ i
        }
        if (c is I3) {
            x = x ^ i
        }
        if (c is I4) {
            x = x &+ i
        }
        if (c is I5) {
            x = x ^ i
        }
        if (c is I6) {
            x = x &+ i
        }
        if (c is I7) {
            x = x ^ i
        }
        if (c is I8) {
            x = x &+ i
        }
        if (c is I9) {
            x = x ^ i
        }
        return x
    }

    func classCast() -> Int {
        let c0: Any = C0()
        let c1: Any = C1()
        let c2: Any = C2()
        let c3: Any = C3()
        let c4: Any = C4()
        let c5: Any = C5()
        let c6: Any = C6()
        let c7: Any = C7()
        let c8: Any = C8()
        let c9: Any = C9()

        var x = 0
        for i in 0..<RunsInfo.RUNS {
            x = x &+ foo_class(c0, x, i)
            x = x &+ foo_class(c1, x, i)
            x = x &+ foo_class(c2, x, i)
            x = x &+ foo_class(c3, x, i)
            x = x &+ foo_class(c4, x, i)
            x = x &+ foo_class(c5, x, i)
            x = x &+ foo_class(c6, x, i)
            x = x &+ foo_class(c7, x, i)
            x = x &+ foo_class(c8, x, i)
            x = x &+ foo_class(c9, x, i)
        }
        return x
    }

    func interfaceCast() -> Int {
        let c0: Any = C0()
        let c1: Any = C1()
        let c2: Any = C2()
        let c3: Any = C3()
        let c4: Any = C4()
        let c5: Any = C5()
        let c6: Any = C6()
        let c7: Any = C7()
        let c8: Any = C8()
        let c9: Any = C9()

        var x = 0
        for i in 0..<RunsInfo.RUNS {
            x = x &+ foo_iface(c0, x, i)
            x = x &+ foo_iface(c1, x, i)
            x = x &+ foo_iface(c2, x, i)
            x = x &+ foo_iface(c3, x, i)
            x = x &+ foo_iface(c4, x, i)
            x = x &+ foo_iface(c5, x, i)
            x = x &+ foo_iface(c6, x, i)
            x = x &+ foo_iface(c7, x, i)
            x = x &+ foo_iface(c8, x, i)
            x = x &+ foo_iface(c9, x, i)
        }
        return x
    }
}
