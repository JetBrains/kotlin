/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class DefaultArgumentBenchmark {
    private var arg = 0
    
    init() {
        arg = Int.random(in: 0...100)
    }
    
    
    func sumTwo(_ first: Int, _ second: Int = 0) -> Int {
        return first + second
    }
    
    
    func sumFour(_ first: Int, _ second: Int = 0, _ third: Int = 1, _ fourth: Int = 1) -> Int {
        return first + second + third + fourth
    }
    
    func sumEight(_ first: Int, _ second: Int = 0, _ third: Int = 1, _ fourth: Int = 1, _ fifth: Int = 1, _ sixth: Int = 1, _ seventh: Int = 1, _ eighth: Int = 1) -> Int {
        return first + second + third + fourth + fifth + sixth + seventh + eighth
    }
    
    func testOneOfTwo() {
        sumTwo(arg)
    }
    
    func testTwoOfTwo() {
        sumTwo(arg, arg)
    }
    
    func testOneOfFour() {
        sumFour(arg)
    }
    
    func testFourOfFour() {
        sumFour(arg, arg, arg, arg)
    }
    
    func testOneOfEight() {
        sumEight(arg)
    }
    
    func testEightOfEight() {
        sumEight(arg, arg, arg, arg, arg, arg, arg, arg)
    }
}
