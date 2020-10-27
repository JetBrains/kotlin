/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class LambdaBenchmark {
    @inlinable
    public func runLambda<T>(_ x: () -> T) -> T {
        return x()
    }
    
    private func runLambdaNoInline<T>(_ x: () -> T) -> T {
        return x()
    }

    init() {
        Constants.globalAddendum = Int.random(in: 0 ..< 20)
    }

    func noncapturingLambda() -> Int {
        var x: Int = 0
        for _ in 0...Constants.BENCHMARK_SIZE {
            x += runLambda { Constants.globalAddendum }
        }
        return x
    }

    func noncapturingLambdaNoInline() -> Int {
        var x: Int = 0
        for _ in 0...Constants.BENCHMARK_SIZE {
            x += runLambdaNoInline { Constants.globalAddendum }
        }
        return x
    }

    func capturingLambda() -> Int {
        let addendum = Constants.globalAddendum + 1
        var x: Int = 0
        for _ in 0...Constants.BENCHMARK_SIZE {
            x += runLambda { addendum }
        }
        return x
    }

    func capturingLambdaNoInline() -> Int {
        let addendum = Constants.globalAddendum + 1
        var x: Int = 0
        for _ in 0...Constants.BENCHMARK_SIZE {
            x += runLambdaNoInline { addendum }
        }
        return x
    }

    func mutatingLambda() -> Int {
        var x: Int = 0
        for _ in 0...Constants.BENCHMARK_SIZE {
            runLambda { x += Constants.globalAddendum }
        }
        return x
    }

    func mutatingLambdaNoInline() -> Int {
        var x: Int = 0
        for _ in 0...Constants.BENCHMARK_SIZE {
            runLambdaNoInline { x += Constants.globalAddendum }
        }
        return x
    }

    func methodReference() -> Int {
        var x: Int = 0
        for _ in 0...Constants.BENCHMARK_SIZE {
            x += runLambda(referenced)
        }
        return x
    }

    func methodReferenceNoInline() -> Int {
        var x: Int = 0
        for _ in 0...Constants.BENCHMARK_SIZE {
            x += runLambdaNoInline(referenced)
        }
        return x
    }
}

private func referenced() -> Int {
    return Constants.globalAddendum
}
