/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class IntStreamBenchmark {
    private var _data: AnySequence<Int>? = nil
    var data: AnySequence<Int> {
        return _data!
    }

    init() {
        _data = AnySequence(intValues(Constants.BENCHMARK_SIZE))
    }

    func copy() -> [Int] {
        return Array(data)
    }
    
    func copyManual() -> [Int] {
        var list: [Int] = []
        for item in data.lazy {
            list.append(item)
        }
        return list
    }
    
    func filterAndCount() -> Int {
        return data.lazy.filter { filterLoad($0) }.count
    }
    
    func filterAndMap() {
        for item in (data.lazy.filter { filterLoad($0) }.map { mapLoad($0) }) {
            Blackhole.consume(item)
        }
    }
    
    func filterAndMapManual() {
        for it in data.lazy {
            if (filterLoad(it)) {
                let item = mapLoad(it)
                Blackhole.consume(item)
            }
        }
    }
    
    func filter() {
        for item in (data.lazy.filter { filterLoad($0) }) {
            Blackhole.consume(item)
        }
    }
    
    func filterManual() {
        for it in data.lazy {
            if (filterLoad(it)) {
                Blackhole.consume(it)
            }
        }
    }
    
    func countFilteredManual() -> Int {
        var count = 0
        for it in data.lazy {
            if (filterLoad(it)) {
                count += 1
            }
        }
        return count
    }
    
    func countFiltered() -> Int {
        return data.lazy.count { filterLoad($0) }
    }
    
    func countFilteredLocal() -> Int {
        return data.lazy.cnt { filterLoad($0) }
    }
    
    func reduce() -> Int {
        return data.lazy.reduce(0) { if (filterLoad($1)) { return $0 + 1 } else {return $0 } }
    }
}

extension LazySequence {
    func count(where test: (Element) throws -> Bool) rethrows -> Int {
        return try self.filter(test).count
    }
}
