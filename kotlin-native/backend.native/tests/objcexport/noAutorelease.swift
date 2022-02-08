import Kt

private class SwiftLivenessTracker {
    class SwiftWeakRef {
        weak var value: AnyObject?
    }

    var weakRefs: [SwiftWeakRef] = []

    func add(_ obj: AnyObject?) {
        try! assertTrue(obj != nil)

        let weakRef = SwiftWeakRef()
        weakRef.value = obj
        weakRefs.append(weakRef)

        try! assertFalse(objectsAreDead())
    }

    func isEmpty() -> Bool {
        return weakRefs.isEmpty
    }

    func objectsAreDead() -> Bool {
        for weakRef in weakRefs {
            if weakRef.value !== nil { return false }
        }
        return true
    }
}

private class NoAutoreleaseSwiftHelper : NoAutoreleaseSendHelper, NoAutoreleaseReceiveHelper {
    fileprivate var swiftLivenessTracker: SwiftLivenessTracker

    init(swiftLivenessTracker: SwiftLivenessTracker) {
        self.swiftLivenessTracker = swiftLivenessTracker
    }

    func sendKotlinObject(kotlinObject: KotlinObject) {
        swiftLivenessTracker.add(kotlinObject)
    }

    func blockReceivingKotlinObject() -> (KotlinObject) -> Void {
        return {
            self.swiftLivenessTracker.add($0)
        }
    }

    func sendSwiftObject(swiftObject: Any) {
        swiftLivenessTracker.add(swiftObject as AnyObject)
    }

    func sendList(list: [Any]) {
    }

    func sendString(string: String) {
    }

    func sendNumber(number: Any) {
        swiftLivenessTracker.add(number as AnyObject)
    }

    func sendBlock(block: @escaping () -> KotlinObject) {
    }

    func sendCompletion(completionHandler: @escaping (Any?, Error?) -> Void) {
        completionHandler(nil, nil)
    }

    let kotlinObject = KotlinObject()
    let swiftObject = SwiftObject2()
    let list = createList()
    let string = createString()
    let number = createNumber()
    lazy var block = createBlock(swiftLivenessTracker: swiftLivenessTracker)

    func receiveKotlinObject() -> KotlinObject {
        let result = kotlinObject
        swiftLivenessTracker.add(result)
        return result
    }

    func receiveSwiftObject() -> Any {
        let result = swiftObject
        swiftLivenessTracker.add(result)
        return result
    }

    func receiveList() -> [Any] {
        return list
    }

    func receiveString() -> String {
        return string
    }

    func receiveNumber() -> Any {
        let result = number
        swiftLivenessTracker.add(result)
        return number
    }

    func receiveBlock() -> (() -> KotlinObject) {
        return block
    }
}

private struct TestFlags {
    var trackSwiftLifetime: Bool = true
    var checkAutorelease: Bool = true

    // When Kotlin refers to a Swift object which refers to a Kotlin object, to reclaim the latter
    // we have to run the GC twice if experimental MM is enabled:
    // first GC finds the Kotlin wrapper of the Swift object and releases it during the sweep phase.
    // The second GC is then able to reclaim the Kotlin object.
    // This behaviour is different from the legacy MM, which calls objc_release right in the middle of the GC,
    // so is able to process its outcome during the same GC.
    var runGCTwice: Bool = false
}

private func testOnce(flags: TestFlags, block: (KotlinLivenessTracker, SwiftLivenessTracker) throws -> Void) throws {
    try assertAutoreleasepoolNotUsed(flags: flags) { // Note: this assertion will also fail if something in the "test infra" below uses autoreleasepool.
        let kotlinLivenessTracker = KotlinLivenessTracker()
        let swiftLivenessTracker = SwiftLivenessTracker()

        try assertTrue(kotlinLivenessTracker.isEmpty())
        try assertTrue(swiftLivenessTracker.isEmpty())

        try block(kotlinLivenessTracker, swiftLivenessTracker)

        NoAutoreleaseKt.gc()
        if (flags.runGCTwice) {
            NoAutoreleaseKt.gc()
        }

        try assertFalse(kotlinLivenessTracker.isEmpty())
        if flags.trackSwiftLifetime {
            try assertFalse(swiftLivenessTracker.isEmpty())
        } else {
            // Note: some of the tests don't actually add objects to the swiftLivenessTracker,
            // because e.g. Swift represents certain Obj-C classes as value types.
            try assertTrue(swiftLivenessTracker.isEmpty())
        }

#if !NOOP_GC
        // If something has "leaked" to autoreleasepool, one of the assertions below should fail:
        try assertTrue(kotlinLivenessTracker.objectsAreDead())
        try assertTrue(swiftLivenessTracker.objectsAreDead())
#endif
    }
}

private func test(flags: TestFlags, block: (KotlinLivenessTracker, SwiftLivenessTracker) -> Void) throws {
    for _ in 1...2 {
        // Repeating twice to cover possible fast paths after caching something for a type.
        try testOnce(flags: flags, block: block)
    }
}

private func assertAutoreleasepoolNotUsed(flags: TestFlags = TestFlags(), block: () throws -> Void) throws {
    let poolBefore = NoAutoreleaseKt.objc_autoreleasePoolPush()
    NoAutoreleaseKt.objc_autoreleasePoolPop(handle: poolBefore)

    try block()

    let poolAfter = NoAutoreleaseKt.objc_autoreleasePoolPush()
    NoAutoreleaseKt.objc_autoreleasePoolPop(handle: poolAfter)

    // autoreleasepool machinery is implemented as a stack.
    // So here we check that stack top after `block()` is the same as before.
    // Which means that no objects were added to this stack inbetween.
    if flags.checkAutorelease {
        try assertEquals(actual: poolAfter, expected: poolBefore)
    } else {
      // Just to ensure we disable the hack once it is not needed anymore:
        try assertFalse(poolAfter == poolBefore)
    }
}

private func testSendToKotlin<T>(
    _ createObject: () -> T,
    flags: TestFlags = TestFlags(),
    sendObject: (NoAutoreleaseKotlinSendHelper, T) -> Void
) throws {
    try test(flags: flags) { kotlinLivenessTracker, swiftLivenessTracker in
        let helper = NoAutoreleaseKotlinSendHelper(kotlinLivenessTracker: kotlinLivenessTracker)

        let obj = createObject()
        if flags.trackSwiftLifetime {
            swiftLivenessTracker.add(obj as AnyObject)
        }

        // Repeating twice to cover possible fast paths after caching something for an object.
        sendObject(helper, obj)
        sendObject(helper, obj)
    }
}

private func testReceiveFromKotlin<T>(flags: TestFlags = TestFlags(), receiveObject: (NoAutoreleaseKotlinReceiveHelper) -> T) throws {
    try test(flags: flags) { kotlinLivenessTracker, swiftLivenessTracker in
        let helper = NoAutoreleaseKotlinReceiveHelper(kotlinLivenessTracker: kotlinLivenessTracker)

        // Repeating twice to cover possible fast paths after caching something for an object.
        let obj1 = receiveObject(helper)
        let obj2 = receiveObject(helper)

        if flags.trackSwiftLifetime {
            swiftLivenessTracker.add(obj1 as AnyObject)
            swiftLivenessTracker.add(obj2 as AnyObject)
        }
    }
}

private func testCallToSwift(flags: TestFlags = TestFlags(), callKotlin: (NoAutoreleaseSwiftHelper, KotlinLivenessTracker) -> Void) throws {
    try test(flags: flags) { kotlinLivenessTracker, swiftLivenessTracker in
        let helper = NoAutoreleaseSwiftHelper(swiftLivenessTracker: swiftLivenessTracker)

        callKotlin(helper, kotlinLivenessTracker)
    }
}

private class SwiftObject1 {}
private class SwiftObject2 {}
private class SwiftObject3 {}
private class SwiftObject4 {}

private func testSendKotlinObjectToKotlin() throws {
    try testSendToKotlin({ KotlinObject() }) {
        $0.sendKotlinObject(kotlinObject: $1)
    }
}

private func testSendKotlinObjectToKotlinBlock() throws {
    try testSendToKotlin({ KotlinObject() }) {
        $0.blockReceivingKotlinObject()($1)
    }
}

private func testSendSwiftObjectToKotlin() throws {
    try testSendToKotlin({ SwiftObject1() }) {
        $0.sendSwiftObject(swiftObject: $1)
    }
}

private func testSendListToKotlin() throws {
    try testSendToKotlin({ createList() }, flags: TestFlags(trackSwiftLifetime: false)) {
        $0.sendList(list: $1)
    }
}

private func testSendStringToKotlin() throws {
    try testSendToKotlin({ createString() }, flags: TestFlags(trackSwiftLifetime: false)) {
        $0.sendString(string: $1)
    }
}

private func testSendNumberToKotlin() throws {
    try testSendToKotlin({ createNumber() }) {
        $0.sendNumber(number: $1)
    }
}

private func testSendBlockToKotlin() throws {
    try testSendToKotlin({ createBlock() }, flags: TestFlags(trackSwiftLifetime: false)) {
        $0.sendBlock(block: $1)
    }
}

private func testSendCompletionToKotlin() throws {
    try testSendToKotlin({ createCompletion() }, flags: TestFlags(trackSwiftLifetime: false)) {
        $0.sendCompletion(completionHandler: $1)
    }
}

private func testReceiveKotlinObjectFromKotlin() throws {
    try testReceiveFromKotlin {
        $0.receiveKotlinObject()
    }
}

private func testReceiveSwiftObjectFromKotlin() throws {
    try testReceiveFromKotlin { (helper: NoAutoreleaseKotlinReceiveHelper) -> Any in
        helper.swiftObject = SwiftObject3()
        return helper.receiveSwiftObject()
    }
}

private func testReceiveListFromKotlin() throws {
    // Swift conversion from NSArray to Swift Array uses non-optimized autorelease in the implementation,
    // So regardless of our efforts, passing List from Kotlin to Swift will use autoreleasepool.
    // Use `checkAutorelease: false` flag to disable the relevant check.
    try testReceiveFromKotlin(flags: TestFlags(trackSwiftLifetime: false, checkAutorelease: false)) {
        $0.receiveList()
    }
}

private func testReceiveStringFromKotlin() throws {
    try testReceiveFromKotlin(flags: TestFlags(trackSwiftLifetime: false)) {
        $0.receiveString()
    }
}

private func testReceiveNumberFromKotlin() throws {
    try testReceiveFromKotlin {
        $0.receiveNumber()
    }
}

private func testReceiveBlockFromKotlin() throws {
    try testReceiveFromKotlin(flags: TestFlags(trackSwiftLifetime: false)) {
        $0.receiveBlock()
    }
}

private func testReceiveBlockFromKotlinAndCall() throws {
    try testReceiveFromKotlin {
        $0.receiveBlock()()
    }
}

private func testCreateKotlinArray() throws {
    try testReceiveFromKotlin { (helper: NoAutoreleaseKotlinReceiveHelper) -> KotlinIntArray in
        let array = KotlinIntArray(size: 2)
        helper.kotlinLivenessTracker.add(obj: array)
        return array
    }
}

private func testGetKotlinUnit() throws {
    try assertAutoreleasepoolNotUsed {
        try assertSame(actual: KotlinUnit.shared, expected: KotlinUnit.shared)
    }
}

private func testGetKotlinSingleton() throws {
    try assertAutoreleasepoolNotUsed {
        try assertEquals(actual: NoAutoreleaseSingleton.shared.x, expected: 1)
    }
}

private func testGetKotlinEnumEntry() throws {
    try assertAutoreleasepoolNotUsed {
        try assertEquals(actual: NoAutoreleaseEnum.entry.x, expected: 2)
    }
}

private func testSendKotlinObjectToSwift() throws {
    try testCallToSwift {
        NoAutoreleaseKt.callSendKotlinObject(helper: $0, tracker: $1)
    }
}

private func testSendKotlinObjectToSwiftBlock() throws {
    try testCallToSwift {
        NoAutoreleaseKt.sendKotlinObjectToBlock(helper: $0, tracker: $1)
    }
}

private func testSendSwiftObjectToSwift() throws {
    try testCallToSwift {
        NoAutoreleaseKt.callSendSwiftObject(helper: $0, tracker: $1, swiftObject: SwiftObject4())
    }
}

private func testSendListToSwift() throws {
    // Swift conversion from NSArray to Swift Array uses non-optimized autorelease in the implementation,
    // So regardless of our efforts, passing List from Kotlin to Swift will use autoreleasepool.
    // Use `checkAutorelease: false` flag to disable the relevant check.
    try testCallToSwift(flags: TestFlags(trackSwiftLifetime: false, checkAutorelease: false)) {
        NoAutoreleaseKt.callSendList(helper: $0, tracker: $1)
    }
}

private func testSendStringToSwift() throws {
    try testCallToSwift(flags: TestFlags(trackSwiftLifetime: false)) {
        NoAutoreleaseKt.callSendString(helper: $0, tracker: $1)
    }
}

private func testSendNumberToSwift() throws {
    try testCallToSwift {
        NoAutoreleaseKt.callSendNumber(helper: $0, tracker: $1)
    }
}

private func testSendBlockToSwift() throws {
    try testCallToSwift(flags: TestFlags(trackSwiftLifetime: false)) {
        NoAutoreleaseKt.callSendBlock(helper: $0, tracker: $1)
    }
}

private func testSendCompletionToSwift() throws {
    try testCallToSwift(flags: TestFlags(trackSwiftLifetime: false)) {
        NoAutoreleaseKt.callSendCompletion(helper: $0, tracker: $1)
    }
}

private func testReceiveKotlinObjectFromSwift() throws {
    try testCallToSwift(flags: TestFlags(runGCTwice: true)) {
        NoAutoreleaseKt.callReceiveKotlinObject(helper: $0, tracker: $1)
    }
}

private func testReceiveSwiftObjectFromSwift() throws {
    try testCallToSwift {
        NoAutoreleaseKt.callReceiveSwiftObject(helper: $0, tracker: $1)
    }
}

private func testReceiveListFromSwift() throws {
    try testCallToSwift(flags: TestFlags(trackSwiftLifetime: false)) {
        NoAutoreleaseKt.callReceiveList(helper: $0, tracker: $1)
    }
}

private func testReceiveStringFromSwift() throws {
    try testCallToSwift(flags: TestFlags(trackSwiftLifetime: false)) {
        NoAutoreleaseKt.callReceiveString(helper: $0, tracker: $1)
    }
}

private func testReceiveNumberFromSwift() throws {
    try testCallToSwift {
        NoAutoreleaseKt.callReceiveNumber(helper: $0, tracker: $1)
    }
}

private func testReceiveBlockFromSwift() throws {
    try testCallToSwift(flags: TestFlags(trackSwiftLifetime: false)) {
        NoAutoreleaseKt.callReceiveBlock(helper: $0, tracker: $1)
    }
}

private func testReceiveBlockFromSwiftAndCall() throws {
    try testCallToSwift(flags: TestFlags(runGCTwice: true)) {
        NoAutoreleaseKt.callReceiveBlockAndCall(helper: $0, tracker: $1)
    }
}

private func createList() -> [Any] {
    return [NSObject()]
}

private func createBlock() -> () -> KotlinObject {
    let blockResult = KotlinObject()
    return { return blockResult } // Make capturing thus dynamic.
}

private func createBlock(swiftLivenessTracker: SwiftLivenessTracker) -> () -> KotlinObject {
    let blockResult = KotlinObject()
    return {
        let result = blockResult // Make capturing thus dynamic.
        swiftLivenessTracker.add(result)
        return result
    }
}

private func createCompletion() -> (Any?, Error?) -> Void {
    return { _, _ in }
}

private func createString() -> String {
    return NSObject().description // Make it dynamic.
}

private func createNumber() -> NSNumber {
    return (0.5 + Double(NSObject().hash)) as NSNumber
}

class NoAutoreleaseTests : SimpleTestProvider {
    override init() {
        super.init()

        test("testSendKotlinObjectToKotlin", testSendKotlinObjectToKotlin)
        test("testSendKotlinObjectToKotlinBlock", testSendKotlinObjectToKotlinBlock)
        test("testSendSwiftObjectToKotlin", testSendSwiftObjectToKotlin)
        test("testSendListToKotlin", testSendListToKotlin)
        test("testSendStringToKotlin", testSendStringToKotlin)
        test("testSendNumberToKotlin", testSendNumberToKotlin)
        test("testSendBlockToKotlin", testSendBlockToKotlin)
        test("testSendCompletionToKotlin", testSendCompletionToKotlin)

        test("testReceiveKotlinObjectFromKotlin", testReceiveKotlinObjectFromKotlin)
        test("testReceiveSwiftObjectFromKotlin", testReceiveSwiftObjectFromKotlin)
        test("testReceiveListFromKotlin", testReceiveListFromKotlin)
        test("testReceiveStringFromKotlin", testReceiveStringFromKotlin)
        test("testReceiveNumberFromKotlin", testReceiveNumberFromKotlin)
        test("testReceiveBlockFromKotlin", testReceiveBlockFromKotlin)
        test("testReceiveBlockFromKotlinAndCall", testReceiveBlockFromKotlinAndCall)

        test("testCreateKotlinArray", testCreateKotlinArray)

        test("testGetKotlinUnit", testGetKotlinUnit)
        test("testGetKotlinSingleton", testGetKotlinSingleton)
        test("testGetKotlinEnumEntry", testGetKotlinEnumEntry)

        test("testSendKotlinObjectToSwift", testSendKotlinObjectToSwift)
        test("testSendKotlinObjectToSwiftBlock", testSendKotlinObjectToSwiftBlock)
        test("testSendSwiftObjectToSwift", testSendSwiftObjectToSwift)
        test("testSendListToSwift", testSendListToSwift)
        test("testSendStringToSwift", testSendStringToSwift)
        test("testSendNumberToSwift", testSendNumberToSwift)
        test("testSendBlockToSwift", testSendBlockToSwift)
        test("testSendCompletionToSwift", testSendCompletionToSwift)

        test("testReceiveKotlinObjectFromSwift", testReceiveKotlinObjectFromSwift)
        test("testReceiveSwiftObjectFromSwift", testReceiveSwiftObjectFromSwift)
        test("testReceiveListFromSwift", testReceiveListFromSwift)
        test("testReceiveStringFromSwift", testReceiveStringFromSwift)
        test("testReceiveNumberFromSwift", testReceiveNumberFromSwift)
        test("testReceiveBlockFromSwift", testReceiveBlockFromSwift)
        test("testReceiveBlockFromSwiftAndCall", testReceiveBlockFromSwiftAndCall)
    }
}
