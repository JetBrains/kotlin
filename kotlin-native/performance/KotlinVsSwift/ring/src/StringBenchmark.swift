/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class StringBenchmark {
    private var _data: [String]? = nil
    var data: [String] {
        get {
            return _data!
        }
    }
    var csv: String = ""

    init() {
        var list: [String] = []
        list.reserveCapacity(Constants.BENCHMARK_SIZE)
        for n in stringValues(Constants.BENCHMARK_SIZE) {
            list.append(n)
        }
        _data = list
        csv = ""
        for _ in 1...Constants.BENCHMARK_SIZE-1 {
            let elem = Double.random(in: 0.0..<100.0)
            csv += String(elem)
            csv += ","
        }
        csv += String(0.0)
    }
    
   
    func stringConcat() -> String? {
        var string: String = ""
        for it in data {
            string += it
        }
        return string
    }
    
    func stringConcatNullable() -> String? {
        var string: String? = ""
        for it in data {
            string? += it
        }
        return string
    }
    
    func stringBuilderConcat() -> String {
        var string: String = ""
        for it in data {
            string += it
        }
        return string
    }
    
    func stringBuilderConcatNullable() -> String {
        var string: String? = ""
        for it in data {
            string? += it
        }
        return string!
    }
    
    func summarizeSplittedCsv() -> Double {
        let fields = csv.split(separator: ",")
        var sum = 0.0
        for field in fields {
            sum += Double(field) ?? 0.0
        }
        return sum
    }
}
