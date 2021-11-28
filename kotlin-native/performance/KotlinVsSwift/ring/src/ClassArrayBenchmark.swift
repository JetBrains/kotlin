/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class ClassArrayBenchmark {
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
    
    func copy() -> [Value] {
        return Array(data)
    }
    
    func copyManual() -> [Value] {
        var list: [Value] = []
        for item in data {
            list.append(item)
        }
        return list
    }
    
    func filterAndCount() -> Int {
        return data.filter { filterLoad($0) }.count
    }
    
    func filterAndMap() -> [String] {
        return data.filter { filterLoad($0) }.map { mapLoad($0) }
    }
    
    func filterAndMapManual() -> [String] {
        var list: [String] = []
        for it in data {
            if (filterLoad(it)) {
                let value = mapLoad(it)
                list.append(value)
            }
        }
        return list
    }
    
    func filter() -> [Value] {
        return data.filter { filterLoad($0) }
    }
    
    func filterManual() -> [Value] {
        var list: [Value] = []
        for it in data {
            if (filterLoad(it)) {
                list.append(it)
            }
        }
        return list
    }
    
    func countFilteredManual() -> Int {
        var count = 0
        for it in data {
            if (filterLoad(it)) {
                count += 1
            }
        }
        return count
    }

    func countFiltered() -> Int {
        return data.count { filterLoad($0) }
    }
    
    func countFilteredLocal() -> Int {
        return data.cnt { filterLoad($0) }
    }
}

extension Collection {
    func count(where test: (Element) throws -> Bool) rethrows -> Int {
        return try self.filter(test).count
    }
}
