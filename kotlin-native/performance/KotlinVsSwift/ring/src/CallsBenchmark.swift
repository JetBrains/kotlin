/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

protocol I {
    func foo() -> Int
}

protocol E {
    func foo() -> Any
}

extension I {
    func foo1() -> Int {
        return 1
    }
    func foo2() -> Int {
        return 2
    }
    func foo3() -> Int {
        return 3
    }
    func foo4() -> Int {
        return 4
    }
    func foo5() -> Int {
        return 5
    }
    func foo6() -> Int {
        return 6
    }
    func foo7() -> Int {
        return 7
    }
    func foo8() -> Int {
        return 8
    }
    func foo9() -> Int {
        return 9
    }
    func foo10() -> Int {
        return 10
    }
    func foo11() -> Int {
        return 11
    }
    func foo12() -> Int {
        return 12
    }
    func foo13() -> Int {
        return 13
    }
    func foo14() -> Int {
        return 14
    }
    func foo15() -> Int {
        return 15
    }
    func foo16() -> Int {
        return 16
    }
    func foo17() -> Int {
        return 17
    }
    func foo18() -> Int {
        return 18
    }
    func foo19() -> Int {
        return 19
    }
    func foo20() -> Int {
        return 20
    }
    func foo21() -> Int {
        return 21
    }
    func foo22() -> Int {
        return 22
    }
    func foo23() -> Int {
        return 23
    }
    func foo24() -> Int {
        return 24
    }
    func foo25() -> Int {
        return 25
    }
    func foo26() -> Int {
        return 26
    }
    func foo27() -> Int {
        return 27
    }
    func foo28() -> Int {
        return 28
    }
    func foo29() -> Int {
        return 29
    }
    func foo30() -> Int {
        return 30
    }
    func foo31() -> Int {
        return 31
    }
    func foo32() -> Int {
        return 32
    }
    func foo33() -> Int {
        return 33
    }
    func foo34() -> Int {
        return 34
    }
    func foo35() -> Int {
        return 35
    }
    func foo36() -> Int {
        return 36
    }
    func foo37() -> Int {
        return 37
    }
    func foo38() -> Int {
        return 38
    }
    func foo39() -> Int {
        return 39
    }
    func foo40() -> Int {
        return 40
    }
    func foo41() -> Int {
        return 41
    }
    func foo42() -> Int {
        return 42
    }
    func foo43() -> Int {
        return 43
    }
    func foo44() -> Int {
        return 44
    }
    func foo45() -> Int {
        return 45
    }
    func foo46() -> Int {
        return 46
    }
    func foo47() -> Int {
        return 47
    }
    func foo48() -> Int {
        return 48
    }
    func foo49() -> Int {
        return 49
    }
    func foo50() -> Int {
        return 50
    }
    func foo51() -> Int {
        return 51
    }
    func foo52() -> Int {
        return 52
    }
    func foo53() -> Int {
        return 53
    }
    func foo54() -> Int {
        return 54
    }
    func foo55() -> Int {
        return 55
    }
    func foo56() -> Int {
        return 56
    }
    func foo57() -> Int {
        return 57
    }
    func foo58() -> Int {
        return 58
    }
    func foo59() -> Int {
        return 59
    }
    func foo60() -> Int {
        return 60
    }
    func foo61() -> Int {
        return 31
    }
    func foo62() -> Int {
        return 62
    }
    func foo63() -> Int {
        return 63
    }
    func foo64() -> Int {
        return 64
    }
    func foo65() -> Int {
        return 65
    }
    func foo66() -> Int {
        return 66
    }
    func foo67() -> Int {
        return 67
    }
    func foo68() -> Int {
        return 68
    }
    func foo69() -> Int {
        return 69
    }
    func foo70() -> Int {
        return 70
    }
    func foo71() -> Int {
        return 71
    }
    func foo72() -> Int {
        return 72
    }
    func foo73() -> Int {
        return 73
    }
    func foo74() -> Int {
        return 74
    }
    func foo75() -> Int {
        return 75
    }
    func foo76() -> Int {
        return 76
    }
    func foo77() -> Int {
        return 77
    }
    func foo78() -> Int {
        return 78
    }
    func foo79() -> Int {
        return 79
    }
    func foo80() -> Int {
        return 80
    }
    func foo81() -> Int {
        return 81
    }
    func foo82() -> Int {
        return 82
    }
    func foo83() -> Int {
        return 83
    }
    func foo84() -> Int {
        return 84
    }
    func foo85() -> Int {
        return 85
    }
    func foo86() -> Int {
        return 86
    }
    func foo87() -> Int {
        return 87
    }
    func foo88() -> Int {
        return 88
    }
    func foo89() -> Int {
        return 89
    }
    func foo90() -> Int {
        return 90
    }
    func foo91() -> Int {
        return 91
    }
    func foo92() -> Int {
        return 92
    }
    func foo93() -> Int {
        return 93
    }
    func foo94() -> Int {
        return 94
    }
    func foo95() -> Int {
        return 95
    }
    func foo96() -> Int {
        return 96
    }
    func foo97() -> Int {
        return 97
    }
    func foo98() -> Int {
        return 98
    }
    func foo99() -> Int {
        return 99
    }

}

protocol A : I {}

class CallsBenchmarks {
    class B : A {
        func foo() -> Int {
            return 42
        }
    }
    class C : A {
        func foo() -> Int {
            return 117
        }
    }
    class D : A {
        func foo() -> Int {
            return 314
        }
    }
    class X : A {
        func foo() -> Int {
            return 456456
        }
    }
    class Y : A {
        func foo() -> Int {
            return -398473
        }
    }
    class Z : A {
        func foo() -> Int {
            return 8298734
        }
    }
    
    let d = D()
    let a1: A = B()
    let a2: A = C()
    lazy var a3: A = d
    lazy var i1: I = a1
    lazy var i2: I = a2
    lazy var i3: I = d
    let i4: I = X()
    let i5: I = Y()
    let i6: I = Z()
    
    func finalMethodCall() -> Int {
        var x = 0
        let d = self.d
        for _ in 0..<Constants.RUNS {
            x += d.foo()
        }
        return x
    }
    
    func classOpenMethodCall_MonomorphicCallsite() -> Int {
        var x = 0
        let a1 = self.a1
        for _ in 0..<Constants.RUNS {
            x += a1.foo()
        }
        return x
    }
    
    func classOpenMethodCall_BimorphicCallsite() -> Int {
        var x = 0
        let a1 = self.a1
        let a2 = self.a2
        for i in 0..<Constants.RUNS {
            x += ((i & 1 == 0) ? a1 : a2).foo()
        }
        return x
    }
    
    func classOpenMethodCall_TrimorphicCallsite() -> Int {
        var x = 0
        
        let a1 = self.a1
        let a2 = self.a2
        let a3 = self.a3
        for i in 0..<Constants.RUNS {
            switch (i % 3) {
                case 1: x += a1.foo()
                case 2: x += a2.foo()
                default: x += a3.foo()
            }
        }
        return x
    }
    
    func interfaceMethodCall_MonomorphicCallsite() -> Int {
        var x = 0
        let i1 = self.i1
        for _ in 0..<Constants.RUNS {
            x += i1.foo()
        }
        return x
    }
    
    func interfaceMethodCall_BimorphicCallsite() -> Int {
        var x = 0
        let i1 = self.i1
        let i2 = self.i2
        for i in 0..<Constants.RUNS {
            x += ((i & 1 == 0) ? i1 : i2).foo()
        }
        return x
    }
    
    func interfaceMethodCall_TrimorphicCallsite() -> Int {
        var x = 0
        let i1 = self.i1
        let i2 = self.i2
        let i3 = self.i3
        for i in 0..<Constants.RUNS {
            switch (i % 3) {
                case 1: x += i1.foo()
                case 2: x += i2.foo()
                default: x += i3.foo()
            }
        }
        return x
    }
    
    func interfaceMethodCall_HexamorphicCallsite() -> Int {
        var x = 0
        let i1 = self.i1
        let i2 = self.i2
        let i3 = self.i3
        let i4 = self.i4
        let i5 = self.i5
        let i6 = self.i6
        for i in 0..<Constants.RUNS {
            switch (i % 6) {
                case 1: x += i1.foo()
                case 2: x += i2.foo()
                case 3: x += i3.foo()
                case 4: x += i4.foo()
                case 5: x += i5.foo()
                default: x += i6.foo()
            }
        }
        return x
    }
        
    class F : E {
        func foo() -> Any {
            return 42
        }
    }
    
    let e: E = F()
    
    func returnBoxUnboxFolding() -> Int {
        var x = 0
        let e = self.e
        for _ in 0..<Constants.RUNS {
            x += e.foo() as! Int
        }
        return x
    }
}
