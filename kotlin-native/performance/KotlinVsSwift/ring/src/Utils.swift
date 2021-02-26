/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class Blackhole {
    @inline(never)
    static var flag: Bool = false
    @inline(never)
    static func consume<T: Any>(_ value: T) {
        if (flag) {
            print(value)
        }
    }
}

struct Constants {
    static let BENCHMARK_SIZE = 10000
    static let RUNS = 1_000_000
    static var globalAddendum = 0
}
