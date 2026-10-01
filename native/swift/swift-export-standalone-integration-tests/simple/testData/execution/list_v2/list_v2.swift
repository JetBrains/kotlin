import KotlinRuntime
import ListExport
import ListExportObjC
import KotlinStdlib
import Testing
import KotlinRuntimeSupport

func assertReversed<T: Equatable>(reversed: any KotlinRuntimeSupport.List<T>, original: any KotlinRuntimeSupport.List<T>) throws {
    let expected = original.reversed()
    try #require(expected is ReversedCollection<AnyList<T>>)
    try #require(reversed.elementsEqual(expected))
}

func assertEquals<T: Equatable>(actual: any KotlinRuntimeSupport.List<T>, expected: any KotlinRuntimeSupport.List<T>) throws {
    try #require(actual.count == expected.count)
    for i in actual.indices {
        try #require(actual[i] == expected[i])
    }
}

let array = [4, 8, 5, 16, 23, 42]

@Test
func testArrayOfInt() throws {
    let original = listOf(elements: Int32(4), 8, 5, 16, 23, 42)
    try assertReversed(reversed: reverseListInt(l: original), original: original)
}

@Test
func testArrayOfShort() throws {
    let original = listOf(elements: Int16(4), 8, 5, 16, 23, 42)
    try assertReversed(reversed: reverseListShort(l: original), original: original)
}

// TODO: kotlin.ClassCastException: class kotlin.Int cannot be cast to class kotlin.Char KT-90618
// @Test
// func testArrayOfChar() throws {
//     let original = Array("AB0-Ыß☺🙂系".utf16)
//     let originalKt = listOf(elements:
//         original[0], original[1], original[2], original[3], original[4],
//         original[5], original[6], original[7], original[8], original[9],
//     )
//     try #require(originalKt.count == original.count)
//     try assertReversed(reversed: reverseListChar(l: originalKt), original: originalKt)
// }

@Test
func testArrayOfString() throws {
    let original = listOf(elements: String(4), String(8), String(5), String(16), String(23), String(42))
    try assertReversed(reversed: reverseListString(l: original), original: original)
}

@Test
func testArrayOfBox() throws {
    let original = listOf(elements: Box(x: 4), Box(x: 8), Box(x: 5), Box(x: 16), Box(x: 23), Box(x: 42))
    try assertReversed(reversed: reverseListBox(l: original), original: original)
}

@Test
func testArrayOfOptInt() throws {
    let original = listOf(elements: Int32(4), 8, 5, 16, 23, 42, nil)
    try assertReversed(reversed: reverseListOptInt(l: original), original: original)
}

@Test
func testArrayOfOptString() throws {
    let original = listOf(elements: String(4), String(8), String(5), String(16), String(23), String(42), nil)
    try assertReversed(reversed: reverseListOptString(l: original), original: original)
}

@Test
func testArrayOfOptBox() throws {
    let original = listOf(elements: Box(x: 4), Box(x: 8), Box(x: 5), Box(x: 16), Box(x: 23), Box(x: 42), nil)
    try assertReversed(reversed: reverseListOptBox(l: original), original: original)
}

// TODO: any List is not Equatable KT-88831
// @Test
// func testArrayOfArray() throws {
//     let original = listOf(elements:
//         listOf(elements: Int32(4)),
//         listOf(elements: Int32(8)),
//         listOf(elements: Int32(5)),
//         listOf(elements: Int32(16)),
//         listOf(elements: Int32(23)),
//         listOf(elements: Int32(42)),
//     )
//     try assertReversed(reversed: reverseListListInt(l: original), original: original)
// }
//
// @Test
// func testArrayOfOptArray() throws {
//     let original = listOf(elements:
//         listOf(elements: Int32(4)),
//         listOf(elements: Int32(8)),
//         listOf(elements: Int32(5)),
//         listOf(elements: Int32(16)),
//         listOf(elements: Int32(23)),
//         listOf(elements: Int32(42)),
//         nil
//     )
//     try assertReversed(reversed: reverseListOptListInt(l: original), original: original)
// }

@Test
func testOptArray() throws {
    try #require(reverseOptListInt(l: .none) == nil)

    let originalSome = listOf(elements: Int32(4), 8, 5, 16, 23, 42)
    try assertReversed(reversed: reverseOptListInt(l: originalSome)!, original: originalSome)
}

// TODO: Vararg of Nothing is not allowed KT-88831
// @Test
// func testArrayOfNever() throws {
//     let neverArray: [Never] = []
//     try #require(reverseListNothing(l: neverArray).isEmpty)
//
//     let optNeverArray: [Never?] = [.none, .none, .none]
//     try assertReversed(reversed: reverseListOptNothing(l: optNeverArray), original: optNeverArray)
// }

@Test
func testExtArrayOrInt() throws {
    let original = listOf(elements: Int32(4), 8, 5, 16, 23, 42)
    try assertReversed(reversed: extReverseListInt(original), original: original)
}

@Test
func testExtArrayOrIntProp() throws {
    let original = listOf(elements: Int32(4), 8, 5, 16, 23, 42)
    try assertReversed(reversed: getExtReverseListIntProp(original), original: original)
}

@Test
func testMutableArrayOfInt() throws {
    let original = mutableListOf(elements: Int32(4), 8, 5, 16, 23, 42)
    original[1] = 64
    let expected = listOf(elements: Int32(4), 64, 5, 16, 23, 42)
    try assertReversed(reversed: reverseListInt(l: original), original: expected)
}

@Test
func testCustomListOfInt() throws {
    let original = customListOf(elements: Int32(4), 8, 5, 16, 23, 42)
    try assertReversed(reversed: reverseCustomListInt(l: original), original: original)
}

class SwiftFoo : Foo {
    private let _value: Int32

    init(_ value: Int32) {
        self._value = value
    }

    func value() -> Int32 {
        _value
    }
}

class SwiftFooArrayProvider : FooArrayProvider {
    private var array: [any Foo] = []

    func getFooArray() -> [any Foo] {
        array
    }

    func setFooArray(_ array: [any Foo]) {
        self.array = array
    }
}

private func testObjCInterop(provider: FooArrayProvider) throws {
    let array: [any Foo] = [SwiftFoo(1), SwiftFoo(2)]
    try #require(array.count == 2)
    try #require(array[0].value() == 1)
    try #require(array[1].value() == 2)

    provider.setFooArray(array)
    let list: any KotlinRuntimeSupport.List<any Foo> = getFooList(provider: provider)
    try #require(list.count == 2)
//     TODO: ObjC protocol types are converted to _KotlinExistential KT-90924
//     try #require(list[0].value() == 1)
//     try #require(list[1].value() == 2)

    let newList = listOf(elements: SwiftFoo(3), SwiftFoo(4), SwiftFoo(5))
    try #require(newList.count == 3)
//     try #require(newList[0].value() == 3)
//     try #require(newList[1].value() == 4)
//     try #require(newList[2].value() == 5)
    setFooList(provider: provider, list: newList)

    let newArray: [any Foo] = provider.getFooArray()
    try #require(newArray.count == 3)
//     try #require(newArray[0].value() == 3)
//     try #require(newArray[1].value() == 4)
//     try #require(newArray[2].value() == 5)
}

@Test
func testObjCInteropObjCImpl() throws {
    try testObjCInterop(provider: ObjCFooArrayProvider())
}

@Test
func testObjCInteropSwiftImpl() throws {
    try testObjCInterop(provider: ObjCFooArrayProvider())
}

@Test
func testMyListImpl() throws {
    let original = myListImplOf(elements: Int32(4), 8, 5, 16, 23, 42)
    try assertReversed(reversed: reverseMyListImpl(l: original), original: original)
}

@Test
func testMyMutableListImpl() throws {
    let original = myMutableListImplOf(elements: Int32(4), 8, 5, 16, 23, 42)
    original[1] = 64
    let expected = listOf(elements: Int32(4), 64, 5, 16, 23, 42)
    try assertReversed(reversed: reverseMyListImpl(l: original), original: expected)
}

@Test
func testBarList() throws {
    let original = barListOf(elements: Bar(value: 4), Bar(value: 8), Bar(value: 5), Bar(value: 16), Bar(value: 23), Bar(value: 42))
    try assertReversed(reversed: reverseBarList(l: original), original: original)
}

@Test
func testMutableBarList() throws {
    let items = [Bar(value: 4), Bar(value: 8), Bar(value: 5), Bar(value: 16), Bar(value: 23), Bar(value: 42)]
    let original = mutableBarListOf(elements: items[0], items[1], items[2], items[3], items[4], items[5])
    let newBar = Bar(value: 64)
    original[1] = newBar
    let expected = barListOf(elements: items[0], newBar, items[2], items[3], items[4], items[5])
    try assertReversed(reversed: reverseBarList(l: original), original: expected)
}

@Test
func testListSubSequenceGet() throws {
    let list = listOf(elements: Int32(4), 8, 5, 16, 23, 42)
    let subList = list[1..<4]
    try #require(subList[2] == 5)
    // subList[2] = 6 // error: cannot assign through subscript: 'subList' is a 'let' constant
}

@Test
func testListSubSequenceSet() throws {
    let list = listOf(elements: Int32(4), 8, 5, 16, 23, 42)
    var subList = list[1..<4]
    try #require(subList[2] == 5)
    subList[2] = 6
    try #require(subList[2] == 6)
    // list is immutable so only the subList should have been changed
    try assertEquals(actual: list, expected: listOf(elements:  Int32(4), 8, 5, 16, 23, 42))
}

@Test
func testMutableListSubSequenceSet() throws {
    let mutableList = mutableListOf(elements: Int32(4), 8, 5, 16, 23, 42)
    var mutableSubList = mutableList[1..<4]
    try #require(mutableSubList[2] == 5)
    mutableSubList[2] = 6
    try #require(mutableSubList[2] == 6)
    // list is mutable and should have been changed
    try assertEquals(actual: mutableList, expected: listOf(elements: Int32(4), 8, 6, 16, 23, 42))
}

@Test
func testListSubSequenceSetRange() throws {
    let list = listOf(elements: Int32(4), 8, 5, 16, 23, 42)
    var subListA = list[0..<3]
    var subListB = list[3..<6]
    try #require(subListA[0] == 4)
    try #require(subListB[3] == 16)
    subListA[0..<3] = subListB
    try #require(subListA[0] == 16)
    // list is immutable so only the subList should have been changed
    try assertEquals(actual: list, expected: listOf(elements: Int32(4), 8, 5, 16, 23, 42))
}

@Test
func testMutableListSubSequenceSetRange() throws {
    let mutableList = mutableListOf(elements: Int32(4), 8, 5, 16, 23, 42)
    var mutableSubListA = mutableList[0..<3]
    var mutableSubListB = mutableList[3..<6]
    try #require(mutableSubListA[0] == 4)
    try #require(mutableSubListB[3] == 16)
    mutableSubListA[0..<3] = mutableSubListB
    try #require(mutableSubListA[0] == 16)
    // list is mutable and should have been changed
    try assertEquals(actual: mutableList, expected: listOf(elements: Int32(16), 23, 42, 16, 23, 42))
}

@Test
func testMutableListSetSubSequence() throws {
    let mutableList = mutableListOf(elements: Int32(4), 8, 5, 16, 23, 42)
    let list = listOf(elements: Int32(9), 6, 2, 7, 3)
    mutableList[2..<5] = list[1..<4]
    try assertEquals(actual: mutableList, expected: listOf(elements: Int32(4), 8, 6, 2, 7, 42))
}

@Test
func testListOfFromStdlib() throws {
    let list = ExportedKotlinPackages.kotlin.collections.listOf(elements: Int32(3), Int32(2), Int32(1))
    try #require((list[0] as? Int32) == 3)
    try #require((list[1] as? Int32) == 2)
    try #require((list[2] as? Int32) == 1)
}
