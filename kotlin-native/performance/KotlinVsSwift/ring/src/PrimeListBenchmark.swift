/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class PrimeListBenchmark {
    private var primes: [Int] = []

    func calcDirect() {
        primes = []
        primes.append(2)
        var i = 3
        while (i <= Constants.BENCHMARK_SIZE) {
            var simple = true
            for prime in primes {
                if (prime * prime > i) {
                    break
                }
                if (i % prime == 0) {
                    simple = false
                    break
                }
            }
            if (simple) {
                primes.append(i)
            }
            i += 2
        }
    }

    func calcEratosthenes() {
        primes = []
        primes.append(contentsOf: 2...Constants.BENCHMARK_SIZE)
        var i = 0
        while (i < primes.count) {
            let divisor = primes[i]
            primes.removeAll(where: { $0 > divisor && $0 % divisor == 0 })
            i += 1
        }
    }
}
