/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation


class ClassListBenchmark {
    private var _data: [Value]? = nil
    var data: [Value] {
        return _data!
    }

    init() {
        var list: [Value] = []
        for n in classValues(Constants.BENCHMARK_SIZE) {
            list.append(n)
        }
        _data = list
    }
    
    func filterAndCountWithLambda() -> Int {
        return data.filter { $0.value % 2 == 0 }.count
    }
    
    func filterWithLambda() -> [Value] {
        return data.filter { $0.value % 2 == 0 }
    }

    func mapWithLambda() -> [String] {
        return data.map { String($0.value) }
    }

    func countWithLambda() -> Int {
        return data.count { $0.value % 2 == 0 }
    }

    func filterAndMapWithLambda() -> [String] {
        return data.filter { $0.value % 2 == 0 }.map { String($0.value) }
    }

    func filterAndMapWithLambdaAsSequence() -> [String] {
        return data.lazy.filter { $0.value % 2 == 0 }.map { String($0.value) }
    }
    
    func reduce() -> Int {
        return data.reduce(0) { if (filterLoad($1)) { return $0 + 1 } else { return $0 }}
    }
}
