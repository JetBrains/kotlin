/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt
import Foundation

private func testOnBackgroundThread() throws {
    let obj = KotlinClassForThreadTest()
    let sem = DispatchSemaphore(value: 0)
    var error: Error? = nil
    
    // DispatchQueue.global runs on a background thread not managed by Kotlin.
    DispatchQueue.global(qos: .background).async {
        do {
            // Trigger description (which calls Kotlin's toString)
            try assertEquals(actual: obj.description, expected: "KotlinClassForThreadTest")
            // Trigger hash
            try assertEquals(actual: obj.hash, expected: 42)
            // Trigger isEqual
            try assertTrue(obj.isEqual(obj))
        } catch let e {
            error = e
        }
        sem.signal()
    }
    
    sem.wait()
    if let e = error {
        throw e
    }
}

class Kt86443Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestOnBackgroundThread", testOnBackgroundThread)
    }
}
