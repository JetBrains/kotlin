/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class LoopBenchmark {
    var array: [Value]

    init() {
        var list: [Value] = []
        for n in classValues(Constants.BENCHMARK_SIZE) {
            list.append(n)
        }
        array = list
    }

    func arrayLoop() {
        for x in array {
            Blackhole.consume(x)
        }
    }

    func arrayIndexLoop() {
        for i in array.indices {
            Blackhole.consume(array[i])
        }
    }

    func rangeLoop() {
        for i in 0...Constants.BENCHMARK_SIZE {
            Blackhole.consume(i)
        }
    }

    func arrayWhileLoop() {
        var i = 0
        let s = array.count
        while (i < s) {
            Blackhole.consume(array[i])
            i += 1
        }
    }

    func arrayForeachLoop() {
        array.forEach { Blackhole.consume($0) }
    }
}
