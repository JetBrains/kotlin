/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class IntBaselineBenchmark {

    func consume() {
        for item in 1...Constants.BENCHMARK_SIZE {
            Blackhole.consume(item)
        }
    }

    func allocateArray() -> [Int] {
        var list: [Int] = []
        list.reserveCapacity(Constants.BENCHMARK_SIZE)
        return list
    }
    
    func allocateArrayAndFill() -> [Int] {
        var list: [Int] = []
        list.reserveCapacity(Constants.BENCHMARK_SIZE)
        for item in 1...Constants.BENCHMARK_SIZE {
            list.append(item)
        }
        return list
    }
}
