import KotlinRuntime
import ListExport
import ListExportObjC
import Testing
import KotlinRuntimeSupport

func assertReversed<T: Equatable>(reversed: any KotlinRuntimeSupport.List<T>, original: any KotlinRuntimeSupport.List<T>) throws {
    try #require(reversed.count == original.count)
    let expected: [T] = original.reversed() // TODO: This is O(n) KT-88831
    for i in reversed.indices {
        try #require(reversed[i] == expected[Int(i)])
    }
}

// TODO: Create generic listOf() and toList functions KT-88831
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

// TODO: kotlin.ClassCastException: class kotlin.Int cannot be cast to class kotlin.Char KT-88831
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

@Test
func testObjCInterop() throws {
    let provider: FooArrayProvider = SwiftFooArrayProvider()

    let array: [any Foo] = [SwiftFoo(1), SwiftFoo(2)]
    try #require(array.count == 2)
    try #require(array[0].value() == 1)
    try #require(array[1].value() == 2)

    provider.setFooArray(array)
    let list: any KotlinRuntimeSupport.List<any Foo> = getFooList(provider: provider)
    try #require(list.count == 2)
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
