/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class ElvisBenchmark {
    
    class Value {
        var value: Int
        init(_ value: Int) {
            self.value = value
        }
    }
    
    var array : [Value?] = []
    
    init() {
        array = (0 ..< Constants.BENCHMARK_SIZE).map { _ in
            if (Int.random(in: 0...Constants.BENCHMARK_SIZE) < Constants.BENCHMARK_SIZE / 10) {
                return nil
            } else {
                return Value(Int.random(in: 0...100))
            }
        }
    }
    
    func testElvis() {
        for obj in array {
            Blackhole.consume(obj?.value ?? 0)
        }
    }
}
