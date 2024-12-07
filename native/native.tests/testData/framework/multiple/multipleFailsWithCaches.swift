/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import First
import Second

// https://youtrack.jetbrains.com/issue/KT-34261
// When First and Second are static frameworks with caches, this test fails due to bad cache isolation:
// Caches included into both frameworks have 'ktypew' globals (with same name, hidden visibility and common linkage)
// for writable part of this "unexposed stdlib class" TypeInfo.
// ld ignores hidden visibility and merges common globals, so two independent frameworks happen to share
// the same global instead of two different globals. Things go wrong at runtime then: this writable TypeInfo part
// is used to store Obj-C class for this Kotlin class. So after the first object is obtained in Swift, both TypeInfos
// have its class, and the second object is wrong then.
func testIsolation4() throws {
    let obj1: Any = First.SharedKt.getUnexposedStdlibClassInstance()
    try assertTrue(obj1 is First.KotlinBase)
    try assertFalse(obj1 is Second.KotlinBase)

// With static caches, after `getUnexposedStdlibClassInstance` invocation above,
// the following two asserts will fail until KT-34261 will be fixed
    let obj2: Any = Second.SharedKt.getUnexposedStdlibClassInstance()
    try assertFalse(obj2 is First.KotlinBase)
    try assertTrue(obj2 is Second.KotlinBase)
}

class MultipleFailsWithCachesTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        tests = [
            TestCase(name: "TestIsolation4", method: withAutorelease(testIsolation4)),
        ]
        providers.append(self)
    }
}