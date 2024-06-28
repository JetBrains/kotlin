/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

func testAccessClassFromLibraryWithShortName() throws {

     let object: MyLibraryA = MyLibraryA(data: "Data from Class")
     let interface: MyLibraryI = MyLibraryA(data: "Data from Interface")
     let enumObject: MyLibraryE = MyLibraryE.b


     let dataFromClass = LibraryKt.readDataFromLibraryClass(input: object)
     let dataFromInterface = LibraryKt.readDataFromLibraryInterface(input: interface)
     let dataFromEnum = LibraryKt.readDataFromLibraryEnum(input: enumObject)

     try assertEquals(actual: dataFromClass, expected: "Data from Class")
     try assertEquals(actual: dataFromInterface, expected: "Data from Interface")
     try assertEquals(actual: dataFromEnum, expected: "Enum entry B")
}

class LibraryTests : SimpleTestProvider {
    override init() {
        super.init()

        test("testAccessClassFromLibraryWithShortName", testAccessClassFromLibraryWithShortName)
    }
}