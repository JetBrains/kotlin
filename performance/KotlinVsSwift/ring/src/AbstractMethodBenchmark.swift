/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class AbstractMethodBenchmark {
    private let arr: [String] = zdf_win
    private let sequence = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
    
    private var sequenceMap: [Character: Int] = [:]
    
    init() {
        var i = 0
        for ch in sequence {
            sequenceMap[ch] = i
            i += 1
        }
    }
    
    func sortStrings() -> Set<String> {
        var size = arr.count
        if (Constants.BENCHMARK_SIZE < size) {
            size = Constants.BENCHMARK_SIZE
        }
        let res = Set(arr[0..<size])
        return res
    }
    
    func sortStringsWithComparator() -> Set<String> {
        var res = Set<String>()
        var size = Constants.BENCHMARK_SIZE < arr.count ? Constants.BENCHMARK_SIZE : arr.count
        arr[0..<size].forEach { (member) in
            res.insert(member)
        }
        return res
    }
}
