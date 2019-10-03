/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation


class ForLoopsBenchmark {
    
    private let array: [Int] = (1...Constants.BENCHMARK_SIZE).map { $0 }
    
    private let charArray: [Character] = (1...Constants.BENCHMARK_SIZE).map { Character(UnicodeScalar($0) ?? "0") }
    
    private lazy var string: String = String(charArray)
    
    private let floatArray: [Float] = (1...Constants.BENCHMARK_SIZE).map { Float($0) }
    
    func arrayLoop() -> Int64 {
        var sum: Int64 = 0
        for e in array {
            sum += Int64(e)
        }
        return sum
    }

    
    func charArrayLoop() -> Int64 {
        var sum : Int64 = 0
        for e in charArray {
            sum += Int64(String(e)) ?? 0
        }
        return sum
    }
    
    func stringLoop() -> Int64 {
        var sum: Int64 = 0
        for e in string {
            sum = sum &+ (Int64(String(e)) ?? 0)
        }
        return sum
    }
    
    func floatArrayLoop() -> Double {
        var sum = 0.0
        for e in floatArray {
            sum += Double(e)
        }
        return sum
    }
    
    func arrayIndicesLoop() -> Int64 {
        var sum = 0
        for i in array.indices {
            sum += array[i]
        }
        return Int64(sum)
    }
    
    func charArrayIndicesLoop() -> Int64 {
        var sum: Int64 = 0
        for i in charArray.indices {
            sum += Int64(String(charArray[i])) ?? 0
        }
        return sum
    }
    
    func stringIndicesLoop() -> Int64 {
        var sum: Int64 = 0
        for i in string.indices {
            sum = sum &+ Int64(string[i].hashValue)
        }
        return sum
    }
    
    func floatArrayIndicesLoop() -> Double {
        var sum = 0.0
        for i in floatArray.indices {
            sum += Double(floatArray[i])
        }
        return sum
    }
}
