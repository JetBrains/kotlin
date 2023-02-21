import Kt

private func testEnumValues() throws {
    let values = EnumLeftRightUpDown.values()

    try assertEquals(actual: values.size, expected: 4)

    try assertSame(actual: values.get(index: 0) as AnyObject, expected: EnumLeftRightUpDown.left)
    try assertSame(actual: values.get(index: 1) as AnyObject, expected: EnumLeftRightUpDown.right)
    try assertSame(actual: values.get(index: 2) as AnyObject, expected: EnumLeftRightUpDown.up)
    try assertSame(actual: values.get(index: 3) as AnyObject, expected: EnumLeftRightUpDown.down)
}

private func testEnumValuesMangled() throws {
    let values = EnumOneTwoThreeValues.values_()

    try assertEquals(actual: values.size, expected: 5)

    try assertSame(actual: values.get(index: 0) as AnyObject, expected: EnumOneTwoThreeValues.one)
    try assertSame(actual: values.get(index: 1) as AnyObject, expected: EnumOneTwoThreeValues.two)
    try assertSame(actual: values.get(index: 2) as AnyObject, expected: EnumOneTwoThreeValues.three)
    try assertSame(actual: values.get(index: 3) as AnyObject, expected: EnumOneTwoThreeValues.values)
    try assertSame(actual: values.get(index: 4) as AnyObject, expected: EnumOneTwoThreeValues.entries)
}

private func testEnumValuesMangledTwice() throws {
    let values = EnumValuesValues_.values__()

    try assertEquals(actual: values.size, expected: 4)

    try assertSame(actual: values.get(index: 0) as AnyObject, expected: EnumValuesValues_.values)
    try assertSame(actual: values.get(index: 1) as AnyObject, expected: EnumValuesValues_.values_)
    try assertSame(actual: values.get(index: 2) as AnyObject, expected: EnumValuesValues_.entries)
    try assertSame(actual: values.get(index: 3) as AnyObject, expected: EnumValuesValues_.entries_)
}

private func testEnumValuesEmpty() throws {
    try assertEquals(actual: EmptyEnum.values().size, expected: 0)
}

extension NSObject {

   // convert to dictionary
   static func toDictionary(from classType: AnyClass) -> [String: Any] {

       var propertiesCount : CUnsignedInt = 0
       let propertiesInAClass = class_copyMethodList(classType, &propertiesCount)
       var propertiesDictionary = [String:Any]()

       for i in 0 ..< Int(propertiesCount) {
          if let property = propertiesInAClass?[i],
             let strKey = NSString(utf8String: sel_getName(method_getName(property))) as String? {
               propertiesDictionary[strKey] = value(forKey: strKey)
          }
       }
       return propertiesDictionary
   }
}


private func testNoEnumEntries() throws {
    try assertTrue(class_respondsToSelector(object_getClass(EnumLeftRightUpDown.self), NSSelectorFromString("entries")));
    try assertFalse(class_respondsToSelector(object_getClass(NoEnumEntriesEnum.self), NSSelectorFromString("entries")));
}

private func testEnumEntries() throws {
    let entries = EnumLeftRightUpDown.entries

    try assertEquals(actual: entries.count, expected: 4)

    try assertSame(actual: entries[0] as AnyObject, expected: EnumLeftRightUpDown.left)
    try assertSame(actual: entries[1] as AnyObject, expected: EnumLeftRightUpDown.right)
    try assertSame(actual: entries[2] as AnyObject, expected: EnumLeftRightUpDown.up)
    try assertSame(actual: entries[3] as AnyObject, expected: EnumLeftRightUpDown.down)
}

private func testEnumEntriesMangled() throws {
    let entries = EnumOneTwoThreeValues.entries_

    try assertEquals(actual: entries.count, expected: 5)

    try assertSame(actual: entries[0] as AnyObject, expected: EnumOneTwoThreeValues.one)
    try assertSame(actual: entries[1] as AnyObject, expected: EnumOneTwoThreeValues.two)
    try assertSame(actual: entries[2] as AnyObject, expected: EnumOneTwoThreeValues.three)
    try assertSame(actual: entries[3] as AnyObject, expected: EnumOneTwoThreeValues.values)
    try assertSame(actual: entries[4] as AnyObject, expected: EnumOneTwoThreeValues.entries)
}

private func testEnumEntriesMangledTwice() throws {
    let entries = EnumValuesValues_.entries__

    try assertEquals(actual: entries.count, expected: 4)

    try assertSame(actual: entries[0] as AnyObject, expected: EnumValuesValues_.values)
    try assertSame(actual: entries[1] as AnyObject, expected: EnumValuesValues_.values_)
    try assertSame(actual: entries[2] as AnyObject, expected: EnumValuesValues_.entries)
    try assertSame(actual: entries[3] as AnyObject, expected: EnumValuesValues_.entries_)
}

private func testEnumEntriesEmpty() throws {
    try assertEquals(actual: EmptyEnum.entries.count, expected: 0)
}

class EnumValuesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestEnumValues", testEnumValues)
        test("TestEnumValuesMangled", testEnumValuesMangled)
        test("TestEnumValuesMangledTwice", testEnumValuesMangledTwice)
        test("TestEnumValuesEmpty", testEnumValuesEmpty)
        test("TestNoEnumEntries", testNoEnumEntries)
        test("TestEnumEntries", testEnumEntries)
        test("TestEnumEntriesMangled", testEnumEntriesMangled)
        test("TestEnumEntriesMangledTwice", testEnumEntriesMangledTwice)
        test("TestEnumEntriesEmpty", testEnumEntriesEmpty)
    }
}
