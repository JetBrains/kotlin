/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation
import Stdlib

extension NSEnumerator {
    func remainingObjects() -> [Any?] {
        var result = [Any?]()
        while (true) {
            if let next = self.nextObject() {
                result.append(next as AnyObject as Any?)
            } else {
                break
            }
        }
        return result
    }
}

class StdlibTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        tests = [
            TestCase(name: "TestEmptyDictionary", method: withAutorelease(testEmptyDictionary)),
            TestCase(name: "TestGenericMapUsage", method: withAutorelease(testGenericMapUsage)),
            TestCase(name: "TestOrderedMapStored", method: withAutorelease(testOrderedMapStored)),
            TestCase(name: "TestTypedMapUsage", method: withAutorelease(testTypedMapUsage)),
            TestCase(name: "TestFirstElement", method: withAutorelease(testFirstElement)),
            TestCase(name: "TestAddDictionary", method: withAutorelease(testAddDictionary)),
            TestCase(name: "TestList", method: withAutorelease(testList)),
            TestCase(name: "TestMutableList", method: withAutorelease(testMutableList)),
            TestCase(name: "TestSet", method: withAutorelease(testSet)),
            TestCase(name: "TestMutableSet", method: withAutorelease(testMutableSet)),
            TestCase(name: "TestMap", method: withAutorelease(testMap)),
            TestCase(name: "TestMutableMap", method: withAutorelease(testMutableMap)),
            TestCase(name: "TestKotlinMutableSetInit", method: withAutorelease(testKotlinMutableSetInit)),
            TestCase(name: "TestKotlinMutableDictionaryInit", method: withAutorelease(testKotlinMutableDictionaryInit)),
            TestCase(name: "TestSwiftSetInKotlin", method: withAutorelease(testSwiftSetInKotlin)),
            TestCase(name: "TestSwiftDictionaryInKotlin", method: withAutorelease(testSwiftDictionaryInKotlin)),
        ]
        providers.append(self)
    }

    /**
     * Pass empty dictionary to Kotlin.
     */
    func testEmptyDictionary() throws {
        let immutableEmptyDict = [String: Int]()
        try assertTrue(StdlibKt.isEmpty(map: immutableEmptyDict), "Empty dictionary")
        let keys = StdlibKt.getKeysAsSet(map: immutableEmptyDict)
        try assertTrue(keys.isEmpty, "Should have empty set")
    }

    /**
     * Tests usage of a map with generics.
     */
    func testGenericMapUsage() throws {
        let map = StdlibKt.createLinkedMap()
        map[1] = "One"
        map[10] = "Ten"
        map[11] = "Eleven"
        map["10"] = "Ten as string"
        for (k, v) in map {
            print("MAP: \(k) - \(v)")
        }

        try assertEquals(actual: map[11] as! String, expected: "Eleven", "An element of the map for key: 11")
    }

    /**
     * Checks order of the underlying LinkedHashMap.
     */
    func testOrderedMapStored() throws {
        let pair = StdlibKt.createPair()
        let map = pair.first as? NSMutableDictionary

        map?[1] = "One"
        map?[10] = "Ten"
        map?[11] = "Eleven"
        map?["10"] = "Ten as string"

        let gen = pair.second as! GenericExtensionClass
        let value: String? = gen.getFirstValue() as? String
        try assertEquals(actual: value!, expected: "One", "First value of the map")

        let key: Int? = gen.getFirstKey() as? Int
        try assertEquals(actual: key!, expected: 1, "First key of the map")
    }

    /**
     * Tests typed map created in Kotlin.
     */
    func testTypedMapUsage() throws {
        let map = StdlibKt.createTypedMutableMap()
        map[1] = "One"
        map[1.0 as Float] = "Float"
        map[11] = "Eleven"
        map["10"] = "Ten as string"
        
        try assertEquals(actual: map["10"] as! String, expected: "Ten as string", "String key")
        try assertEquals(actual: map[1.0 as Float] as! String, expected: "Float", "Float key")
    }
    
    /**
     * Get first element of the collection.
     */
    func testFirstElement() throws {
        let m = StdlibKt.createTypedMutableMap()
        m[10] = "Str"
        try assertEquals(actual: StdlibKt.getFirstElement(collection: m.allKeys) as! Int, expected: 10, "First key")

        try assertEquals(actual: StdlibKt.getFirstElement(collection: StdlibKt.getKeysAsList(map: m as! Dictionary)) as! Int,
                expected: 10, "First key from a list")
    }

    /**
     * Add element to dictionary in Kotlin
     */
    func testAddDictionary() throws {
        let m = [ "ABC": 10, "CDE": 12, "FGH": 3 ]
        StdlibKt.addSomeElementsToMap(map: KotlinMutableDictionary(dictionary: m))
        for (k, v) in m {
            print("MAP: \(k) - \(v)")
        }

        var smd = KotlinMutableDictionary<NSString, KotlinInt>()
        smd.setObject(333, forKey: "333" as NSString)
        try assertEquals(actual: smd.object(forKey: "333" as NSString) as! Int, expected: 333, "Add element to dict")
        
        StdlibKt.addSomeElementsToMap(map: smd)
        for (k, v) in smd {
            print("MAP: \(k) - \(v)")
        }
        try assertEquals(actual: smd.object(forKey: "XYZ" as NSString) as! Int, expected: 321, "Get element from Kotlin")
    }

    func zeroTo(_ n: Int32) -> KotlinArray<AnyObject> { return KotlinArray<AnyObject>(size: n) { $0 } }

    func testList() throws {
        let elements = zeroTo(5)
        elements.set(index: 1, value: nil)
        let list = StdlibKt.list(elements: elements) as! NSArray
        try assertEquals(actual: list.object(at: 2) as! NSNumber, expected: NSNumber(value: 2))
        try assertEquals(actual: list.object(at: 1) as! NSNull, expected: NSNull())
        try assertEquals(actual: list.count, expected: 5)
    }

    func testMutableList() throws {
        let kotlinList = StdlibKt.emptyMutableList() as! NSMutableArray
        let nsList = NSMutableArray()

        func apply<T : Equatable>(op: (NSMutableArray)->T) throws {
            let actual = op(kotlinList)
            let expected = op(nsList)
            try assertEquals(actual: actual, expected: expected)
            try assertEquals(actual: kotlinList, expected: nsList)
            try assertEquals(actual: kotlinList.hash, expected: nsList.hash)
        }

        func applyVoid(op: (NSMutableArray)->Void) throws {
            op(kotlinList)
            op(nsList)
            try assertEquals(actual: kotlinList, expected: nsList)
            try assertEquals(actual: kotlinList.hash, expected: nsList.hash)
        }

        try apply { $0.count }
        try applyVoid { $0.insert(0, at: 0) }
        try applyVoid { $0.insert(1, at: 0) }
        try applyVoid { $0.insert(2, at: 1) }
        try applyVoid { $0.removeObject(at: 0) }
        try applyVoid { $0.add("foo") }
        try applyVoid { $0.removeLastObject() }
        try applyVoid { $0.replaceObject(at: 0, with: "bar") }
        let NULL: Any? = nil
        try applyVoid { $0.add(NULL as Any) }
        try applyVoid { $0.insert(NULL as Any, at: 2) }
        try applyVoid { $0.replaceObject(at: 1, with: NULL as Any) }
        try apply { $0.count }
    }

    func testMutableSet() throws {
        let kotlinSet = StdlibKt.emptyMutableSet() as! NSMutableSet
        let nsSet = NSMutableSet()

        func apply<T : Equatable>(op: (NSMutableSet)->T) throws {
            let actual = op(kotlinSet)
            let expected = op(nsSet)
            try assertEquals(actual: actual, expected: expected)
            try assertEquals(actual: kotlinSet, expected: nsSet)
            try assertEquals(actual: kotlinSet.hash, expected: nsSet.hash)
        }

        func applyVoid(op: (NSMutableSet)->Void) throws {
            op(kotlinSet)
            op(nsSet)
            try assertEquals(actual: kotlinSet, expected: nsSet)
            try assertEquals(actual: kotlinSet.hash, expected: nsSet.hash)
        }

        try apply { $0.count }
        try applyVoid { $0.add("foo") }
        try applyVoid { $0.add("bar") }
        try applyVoid { $0.remove("baz") }
        try applyVoid { $0.add("baz") }
        try applyVoid { $0.add(TripleVals<NSNumber>(first: 1, second: 2, third: 3)) }
        try apply { $0.member(TripleVals<NSNumber>(first: 1, second: 2, third: 3)) as! TripleVals<NSNumber> }
        try apply { $0.member(42) == nil }
        try applyVoid { $0.remove(TripleVals<NSNumber>(first: 1, second: 2, third: 3)) }

        let NULL0: Any? = nil
        let NULL = NULL0 as Any

        try applyVoid { $0.add(NULL) }
        try apply { $0.member(NULL) == nil }
        try apply { $0.member(NULL) as! NSObject }
        try applyVoid { $0.remove(NULL) }
        try apply { $0.member(NULL) == nil }

        try apply { NSSet(array: $0.objectEnumerator().remainingObjects()) }

        try apply { $0.count }
    }

    func testMutableMap() throws {
        // TODO: test KotlinMutableSet/Dictionary constructors
        let kotlinMap = StdlibKt.emptyMutableMap() as! NSMutableDictionary
        let nsMap = NSMutableDictionary()

        func apply<T : Equatable>(op: (NSMutableDictionary)->T) throws {
            let actual = op(kotlinMap)
            let expected = op(nsMap)
            try assertEquals(actual: actual, expected: expected)
            try assertEquals(actual: kotlinMap, expected: nsMap)
            try assertEquals(actual: kotlinMap.hash, expected: nsMap.hash)
        }

        func applyVoid(op: (NSMutableDictionary) throws -> Void) throws {
            try op(kotlinMap)
            try op(nsMap)
            try assertEquals(actual: kotlinMap, expected: nsMap)
            try assertEquals(actual: kotlinMap.hash, expected: nsMap.hash)
        }

        try apply { $0.count }
        try apply { $0.object(forKey: 42) == nil }
        try applyVoid { $0.setObject(42, forKey: 42 as NSNumber) }
        try applyVoid { $0.setObject(17, forKey: "foo" as NSString) }
        let triple = TripleVals<NSNumber>(first: 3, second: 2, third: 1)
        try applyVoid { $0.setObject("bar", forKey: triple) }
        try applyVoid { $0.removeObject(forKey: 42) }
        try apply { $0.count }
        try apply { $0.object(forKey: 42) == nil }
        try apply { $0.object(forKey: "foo") as! NSObject  }
        try apply { $0.object(forKey: triple) as! NSObject }

        try apply { NSSet(array: $0.keyEnumerator().remainingObjects()) }

        let NULL0: Any? = nil
        let NULL = NULL0 as Any

        try apply { $0.object(forKey: NULL) == nil }


        try applyVoid { $0.setObject(42, forKey: NULL as! NSCopying) }
        try applyVoid { $0.setObject(NULL, forKey: "baz" as NSString) }
        try apply { $0.object(forKey: NULL) as! NSObject }
        try apply { $0.object(forKey: "baz") as! NSObject }

        try apply { NSSet(array: $0.keyEnumerator().remainingObjects()) }

        try applyVoid { $0.removeObject(forKey: NULL) }
        try applyVoid { $0.removeObject(forKey: "baz") }

        try applyVoid { $0.removeAllObjects() }

        let myKey = MyKey()
        try applyVoid { $0.setObject(myKey, forKey: myKey) }
        try applyVoid {
            let key = $0.allKeys[0] as! MyKey
            let value = $0.allValues[0] as! MyKey
            try assertFalse(key === myKey)
            try assertTrue(value === myKey)
        }

    }

    @objc class MyKey : NSObject, NSCopying {
        override var hash: Int {
            return 42
        }

        override func isEqual(_ object: Any?) -> Bool {
            return object is MyKey
        }

        func copy(with: NSZone? = nil) -> Any {
            return MyKey()
        }

    }

    func testSet() throws {
        let elements = KotlinArray<AnyObject>(size: 2) { index in nil }
        elements.set(index: 0, value: nil)
        elements.set(index: 1, value: 42 as NSNumber)
        let set = StdlibKt.set(elements: elements) as! NSSet
        try assertEquals(actual: set.count, expected: 2)
        try assertEquals(actual: set.member(NSNull()) as! NSNull, expected: NSNull())
        try assertEquals(actual: set.member(42) as! NSNumber, expected: NSNumber(value: 42 as Int32))
        try assertTrue(set.member(17) == nil)
        try assertFalse(set.member(42) as AnyObject === NSNumber(value: 42 as Int32))
        try assertTrue(set.contains(42))
        try assertTrue(set.contains(nil as Any?))
        try assertFalse(set.contains(17))

        try assertEquals(actual: NSSet(array: set.objectEnumerator().remainingObjects()), expected: NSSet(array: [nil, 42] as [AnyObject]))
    }

    func testMap() throws {
        let elements = KotlinArray<AnyObject>(size: 6) { index in nil }
        elements.set(index: 0, value: nil)
        elements.set(index: 1, value: 42 as NSNumber)
        elements.set(index: 2, value: "foo" as NSString)
        elements.set(index: 3, value: "bar" as NSString)
        elements.set(index: 4, value: 42 as NSNumber)
        elements.set(index: 5, value: nil)

        let map = StdlibKt.map(keysAndValues: elements) as! NSDictionary
        try assertEquals(actual: map.count, expected: 3)

        try assertEquals(actual: map.object(forKey: NSNull()) as! NSNumber, expected: NSNumber(value: 42))
        try assertEquals(actual: map.object(forKey: "foo") as! String, expected: "bar")
        try assertEquals(actual: map.object(forKey: 42) as! NSNull, expected: NSNull())
        try assertTrue(map.object(forKey: "bar") == nil)

        try assertEquals(actual: NSSet(array: map.keyEnumerator().remainingObjects()), expected: NSSet(array: [nil, 42, "foo"] as [AnyObject]))
    }

    func testKotlinMutableSetInit() throws {
        func test(
                _ set: KotlinMutableSet<NSString>,
                _ check: (KotlinMutableSet<NSString>) throws -> Void = { _ in }
        ) throws {
            try assertEquals(actual: String(describing: type(of: set)), expected: "StdlibMutableSet")
            try check(set)
            try assertFalse(set.contains("1"))
            set.add("1")
            try assertTrue(set.contains("1"))
        }

        try test(KotlinMutableSet())
        try test(KotlinMutableSet(capacity: 1))
        try test(KotlinMutableSet(object: "2")) {
            try assertTrue($0.contains("2"))
        }

        var threeAndFour = ["3", "4"] as [AnyObject]
        try test(KotlinMutableSet(objects: &threeAndFour, count: 2)) {
            try assertTrue($0.contains("3"))
            try assertTrue($0.contains("4"))
        }

        try test(KotlinMutableSet(array: ["5", "6"])) {
            try assertTrue($0.contains("5"))
            try assertTrue($0.contains("6"))
        }

        try test(KotlinMutableSet(set: ["7", "8"])) {
            try assertTrue($0.contains("7"))
            try assertTrue($0.contains("8"))
        }

        for flag in [false, true] {
            try test(KotlinMutableSet(set: ["9", "10"], copyItems: flag)) {
                try assertTrue($0.contains("9"))
                try assertTrue($0.contains("10"))
            }
        }

        /*
        TODO: doesn't work, KotlinMutableSet seems to be serialized as NSMutableSet.
        if #available(macOS 10.13, *) {
            let data = try! NSKeyedArchiver.archivedData(
                    withRootObject: KotlinMutableSet<NSString>(array: ["11", "12"]),
                    requiringSecureCoding: false
            )

            try test(try! NSKeyedUnarchiver.unarchivedObject(ofClass: KotlinMutableSet.self, from: data)!) {
                try assertTrue($0.contains("11"))
                try assertTrue($0.contains("12"))
            }
        }
        */

        StdlibKt.gc() // To reproduce https://github.com/JetBrains/kotlin-native/issues/3259
    }

    func testKotlinMutableDictionaryInit() throws {
        func test(
                _ dict: KotlinMutableDictionary<NSString, NSString>,
                _ check: (KotlinMutableDictionary<NSString, NSString>) throws -> Void = { _ in }
        ) throws {
            try assertEquals(actual: String(describing: type(of: dict)), expected: "StdlibMutableDictionary")
            try check(dict)
            try assertTrue(dict["1"] == nil)
            dict["1"] = "2"
            try assertTrue(dict["1"] as? NSString == "2")
        }

        try test(KotlinMutableDictionary())
        try test(KotlinMutableDictionary(capacity: 4))

        // TODO: test [initWithCoder:].

        try test(KotlinMutableDictionary(objects: ["3", "4"], forKeys: ["4", "3"] as [NSString])) {
            try assertEquals(actual: $0["3"] as? String, expected: "4")
            try assertEquals(actual: $0["4"] as? String, expected: "3")
        }

        var fiveAndSix = ["5", "6"] as [AnyObject]
        var sixAndFive = ["6", "5"] as [NSCopying]
        try test(KotlinMutableDictionary(objects: &fiveAndSix, forKeys: &sixAndFive, count: 2)) {
            try assertEquals(actual: $0["5"] as? String, expected: "6")
            try assertEquals(actual: $0["6"] as? String, expected: "5")
        }

        try test(KotlinMutableDictionary(object: "7", forKey: "8" as NSString)) {
            try assertEquals(actual: $0["8"] as? String, expected: "7")
        }

        try test(KotlinMutableDictionary(dictionary: ["10" : "9"])) {
            try assertEquals(actual: $0["10"] as? String, expected: "9")
        }

        for flag in [false, true] {
            try test(KotlinMutableDictionary(dictionary: ["12" : "11"], copyItems: flag)) {
                try assertEquals(actual: $0["12"] as? String, expected: "11")
            }
        }

        try test(KotlinMutableDictionary(dictionaryLiteral: ("14", "13"))) {
            try assertEquals(actual: $0["14"] as? String, expected: "13")
        }

        StdlibKt.gc() // To reproduce https://github.com/JetBrains/kotlin-native/issues/3259
    }

    func testSwiftSetInKotlin() throws {
        try StdlibKt.testSet(set: ["a", "b", "c", "d", "e", "f", "g"])
    }

    func testSwiftDictionaryInKotlin() throws {
        try StdlibKt.testMap(map: ["a" : 1, "b" : 2, "c" : 3, "d" : 4, "e" : 5, "f" : 6, "g" : 7])
    }

}
