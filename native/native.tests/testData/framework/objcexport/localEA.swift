/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

// -------- Tests --------

func testArraysEscapeAsParameter() throws {
    let array1 = ArraysConstructor(int1: 1, int2: 2)
    try assertEquals(actual: array1.log(), expected: "size: 2, contents: [1, 2]", "Wrong array values in class ArraysConstructor.")
    array1.set(int1: 3, int2: 4)
    try assertEquals(actual: array1.log(), expected: "size: 2, contents: [3, 4]", "Wrong array values in class ArraysConstructor.")

    let array2 = ArraysDefault(int1: 1, int2: 2)
    try assertEquals(actual: array2.log(), expected: "size: 2, contents: [1, 2]", "Wrong array values in class ArraysDefault.")
    array2.set(int1: 3, int2: 4)
    try assertEquals(actual: array2.log(), expected: "size: 2, contents: [3, 4]", "Wrong array values in class ArraysDefault.")

    let array3 = ArraysInitBlock(int1: 1, int2: 2)
    try assertEquals(actual: array3.log(), expected: "size: 2, contents: [1, 2]", "Wrong array values in class ArraysInitBlock.")
    array3.set(int1: 3, int2: 4)
    try assertEquals(actual: array3.log(), expected: "size: 2, contents: [3, 4]", "Wrong array values in class ArraysInitBlock.")
}

// -------- Execution of the test --------

class LocalEATests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestArraysEscapeAsParameter", testArraysEscapeAsParameter)
    }
}