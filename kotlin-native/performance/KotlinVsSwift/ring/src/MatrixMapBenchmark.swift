/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class Pair: Hashable {
    
    init(_ pair: (Int, Int)) {
        self.pair = pair
    }
    
    public var pair: (Int, Int)
    public func hash(into hasher: inout Hasher) {
        hasher.combine(pair.0)
        hasher.combine(pair.1)
    }
    
    public static func == (lhs: Pair, rhs: Pair) -> Bool {
        return lhs.pair == rhs.pair
    }
    
    public var hashValue: Int {
        var hasher = Hasher()
        self.hash(into: &hasher)
        return hasher.finalize()
    }
}

class KMatrix {
    let rows: Int
    let columns: Int
    internal init(_ rows: Int, _ columns: Int) {
        self.rows = rows
        self.columns = columns
        for row in 0...rows-1 {
            for col in 0...columns-1 {
                matrix[Pair((row, col))] = Double.random(in: 0 ..< 100)
            }
        }
    }
    
    private var matrix: [Pair: Double] = [:]

    func get(row: Int, col: Int) -> Double {
        return get(Pair((row, col)))
    }

    func get(_ pair: Pair) -> Double {
        return matrix[pair] ??  0.0
    }

    func put(_ pair: Pair, _ elem: Double) {
        matrix[pair] = elem
    }

    static func +=(lhs: inout KMatrix, rhs:KMatrix) {
        for entry in lhs.matrix {
            lhs.put(entry.key, entry.value + rhs.get(entry.key))
        }
    }
}

class MatrixMapBenchmark {

    func add() -> KMatrix {
        var rows = Constants.BENCHMARK_SIZE
        var cols = 1
        while (rows > cols) {
            rows /= 2
            cols *= 2
        }
        var a = KMatrix(rows, cols)
        let b = KMatrix(rows, cols)
        a += b
        return a
    }

}
