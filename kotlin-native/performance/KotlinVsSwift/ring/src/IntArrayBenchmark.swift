/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class IntArrayBenchmark {
    private var _data: [Int]? = nil
    var data: [Int] {
        get {
            return _data!
        }
    }

    init() {
        var list: [Int] = []
        list.reserveCapacity(Constants.BENCHMARK_SIZE)
        for n in intValues(Constants.BENCHMARK_SIZE) {
            list.append(n)
        }
        _data = list
    }

    func copy() -> [Int] {
        return Array(data)
    }

    func copyManual() -> [Int] {
        var list: [Int] = []
        for item in data {
            list.append(item)
        }
        return list
    }

    func filterAndCount() -> Int {
        return data.filter { filterLoad($0) }.count
    }

    func filterSomeAndCount() -> Int {
        return data.filter { Ring.filterSome($0) }.count
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

    func filter() -> [Int] {
        return data.filter { filterLoad($0) }
    }

    func filterSome() -> [Int] {
        return data.filter { Ring.filterSome($0) }
    }

    func filterPrime() -> [Int] {
        return data.filter { Ring.filterPrime($0) }
    }

    func filterManual() -> [Int] {
        var list: [Int] = []
        for it in data {
            if (filterLoad(it)) {
                list.append(it)
            }
        }
        return list
    }

    func filterSomeManual() -> [Int] {
        var list: [Int] = []
        for it in data {
            if (Ring.filterSome(it)) {
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

    func countFilteredSomeManual() -> Int {
        var count = 0
        for it in data {
            if (Ring.filterSome(it)) {
                count += 1
            }
        }
        return count
    }

    func countFilteredPrimeManual() -> Int {
        var count = 0
        for it in data {
            if (Ring.filterPrime(it)) {
                count += 1
            }
        }
        return count
    }

    func countFiltered() -> Int {
        return data.count { filterLoad($0) }
    }

    func countFilteredSome() -> Int {
        return data.count { Ring.filterSome($0) }
    }

    func countFilteredPrime() -> Int {
        let res = data.count { Ring.filterPrime($0) }
        return res
    }

    func countFilteredLocal() -> Int {
        return data.cnt { filterLoad($0) }
    }

    func countFilteredSomeLocal() -> Int {
        return data.cnt { Ring.filterSome($0) }
    }

    func reduce() -> Int {
        return data.reduce(0) { if (filterLoad($1)) { return $0 + 1 } else { return $0 } }
    }
}
