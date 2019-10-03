/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class FibonacciBenchmark {
    func calcClassic() -> Int64 {
        var a: Int64 = 1
        var b: Int64 = 2
        let size = Constants.BENCHMARK_SIZE
        for _ in 0...size-1 {
            let next = a &+ b
            a = b
            b = next
        }
        return b
    }
    
    func calc() -> Int64 {
        var a: Int64 = 1
        var b: Int64 = 2
        for _ in stride(from: Constants.BENCHMARK_SIZE, through: 1, by: -1) {
            let next = a &+ b
            a = b
            b = next
        }
        return b
    }
    
    func calcWithProgression() -> Int64 {
        var a: Int64 = 1
        var b: Int64 = 2
        for _ in stride(from: 1, through: 2*Constants.BENCHMARK_SIZE-1, by: 2) {
            let next = a &+ b
            a = b
            b = next
        }
        return b
    }
    
    func calcSquare() -> Int64 {
        var a: Int64 = 1
        var b: Int64 = 2
        let s = Int64(Constants.BENCHMARK_SIZE)
        let limit = s*s
        
        for _ in stride(from: limit, through: 1, by: -1) {
            let next = a &+ b
            a = b
            b = next
        }
        return b
    }
}
