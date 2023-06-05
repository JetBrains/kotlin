import Foundation
import StacktraceBridges

class Bar : Foo {
    override func foo() -> Array<String> {
        return StacktraceBridgesKt.getStackTrace()
    }
}

func testDirectObjc2Kotlin() throws {
    let trace = StacktraceBridgesKt.getStackTrace()
    try assertTrue(trace[4].contains("objc2kotlin_kfun:#getStackTrace(){}kotlin.collections.List<kotlin.String>"))
}

func testVirtualObjc2Kotlin() throws {
    let trace = StacktraceBridgesKt.createBar().foo()
    try assertTrue(trace[6].contains("objc2kotlin_virtual_kfun:Foo#foo(){}kotlin.collections.List<kotlin.String>"))
}

func testKotlin2Objc() throws {
    let trace = StacktraceBridgesKt.use(foo: Bar())
    try assertTrue(trace[8].contains("kotlin2objc_kfun:Foo#foo(){}kotlin.collections.List<kotlin.String>"))
}

func testCompanionObject() throws {
    let trace = WithCompanion.companion.trace
    try assertTrue(trace[8].contains("objc2kotlin_kclass:WithCompanion#companion"))
}

func testStandaloneObject() throws {
    let trace = Object.shared.trace
    try assertTrue(trace[8].contains("objc2kotlin_kclass:Object#shared"))
}

func testEnumEntry() throws {
    let trace = E.a.trace
    try assertTrue(trace[8].contains("objc2kotlin_kclass:E.A"))
}

class StacktraceBridgesTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "Direct objc2kotlin", method: withAutorelease(testDirectObjc2Kotlin)),
            TestCase(name: "Virtual objc2kotlin", method: withAutorelease(testVirtualObjc2Kotlin)),
            TestCase(name: "kotlin2objc", method: withAutorelease(testKotlin2Objc)),
            TestCase(name: "Companion object", method: withAutorelease(testCompanionObject)),
            TestCase(name: "Standalone object", method: withAutorelease(testStandaloneObject)),
            TestCase(name: "Enum entry", method: withAutorelease(testEnumEntry)),
        ]
    }
}