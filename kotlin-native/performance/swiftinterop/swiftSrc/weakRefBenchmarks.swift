/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import benchmark

private let REPEAT_COUNT = 10000

private class ReferenceWrapper {
    private weak var weakRef: KotlinData?
    private var strongRef: KotlinData?

    init() {
        let data = KotlinData(x: Int32.random(in: 1 ... 1000))
        self.strongRef = data
        self.weakRef = data
    }

    // This function must not be inlined. We must ensure `weakRef` is accessed at each loop iteration.
    @inline(never) func value() -> Int32 {
        let ref: KotlinData? = self.weakRef
        if ref == nil {
            return 0
        }
        return ref!.x
    }

    func dispose() {
        self.strongRef = nil
    }

    func stress() -> Int32 {
        var counter: Int32 = 0
        for _ in 1...REPEAT_COUNT {
            counter += self.value()
        }
        return counter
    }
}

private func deadReferenceWrapper() -> ReferenceWrapper {
    let ref = ReferenceWrapper()
    ref.dispose()
    GCKt.collect()
    return ref
}

class WeakRefBenchmark {
    private let weight = (0..<10_000).map { _ in ReferenceWrapper() }

    private let aliveRef = ReferenceWrapper()
    private let deadRef = deadReferenceWrapper()

    // Access alive reference.
    func aliveReference() {
        let counter = aliveRef.stress()
        if counter == 0 {
            fatalError()
        }
    }

    // Access dead reference.
    func deadReference() {
        let counter = deadRef.stress()
        if counter != 0 {
            fatalError()
        }
    }

    // Access reference that is nulled out in the middle.
    func dyingReference() {
        let ref = ReferenceWrapper()

        ref.dispose()
        GCKt.schedule()

        let counter = ref.stress()
        Blackhole.companion.consume(value: counter)
    }

    func clean() {
        weight.forEach { $0.dispose() }
    }
}
