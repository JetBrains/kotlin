/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class ClassBaselineBenchmark {

    func consume() {
        for item in 1...Constants.BENCHMARK_SIZE {
            Blackhole.consume(Value(item))
        }
    }
    
    func consumeField() {
        let value = Value(0)
        for item in 1...Constants.BENCHMARK_SIZE {
            value.value = item
            Blackhole.consume(value)
        }
    }
    
    func allocateList() -> [Value] {
        let list = [Value](repeating: Value(0), count: Constants.BENCHMARK_SIZE)
        return list
    }
    
    func allocateArray() -> [Value?] {
        let list = [Value?](repeating: nil, count: Constants.BENCHMARK_SIZE)
        return list
    }
    
    func allocateListAndFill() -> [Value] {
        var list: [Value] = []
        for item in 1...Constants.BENCHMARK_SIZE {
            list.append(Value(item))
        }
        return list
    }
    
    func allocateListAndWrite() -> [Value] {
        let value = Value(0)
        var list: [Value] = []
        for _ in 1...Constants.BENCHMARK_SIZE {
            list.append(value)
        }
        return list
    }
    
    func allocateArrayAndFill() -> [Value?] {
        var list = [Value?](repeating: nil, count: Constants.BENCHMARK_SIZE)
        var index = 0
        for item in 1...Constants.BENCHMARK_SIZE {
            list[index] = Value(item)
            index += 1
        }
        return list
    }
}
