/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class WithIndiciesBenchmark {
    private var _data: [Value]? = nil
    var data: [Value] {
        get {
            return _data!
        }
    }

    init() {
        var list: [Value] = []
        for n in classValues(Constants.BENCHMARK_SIZE) {
            list.append(n)
        }
        _data = list
    }

    func withIndicies() {
        for (index, value) in data.lazy.enumerated() {
            if (filterLoad(value)) {
                Blackhole.consume(index)
                Blackhole.consume(value)
            }
        }
    }

    func withIndiciesManual() {
        var index = 0
        for value in data {
            if (filterLoad(value)) {
                Blackhole.consume(index)
                Blackhole.consume(value)
            }
            index += 1
        }
    }
}
