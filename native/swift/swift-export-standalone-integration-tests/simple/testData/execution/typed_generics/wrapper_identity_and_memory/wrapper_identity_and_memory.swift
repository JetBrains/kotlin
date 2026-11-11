import Foundation
import KotlinRuntime
import KotlinRuntimeSupport
import Testing
import WrapperIdentity

// A Swift class with type parameters can't be the cached wrapper of a Kotlin object: the runtime finds the class of
// a wrapper by the Kotlin class, which has no type arguments. These tests check what such wrappers must still give:
// one Swift object per Kotlin object and static type, and no memory left behind.

@Test
func kotlinObjectOfGenericClassIsOneSwiftObject() throws {
    #expect(finalBox() === finalBox())
    #expect(openBox() === openBox())
    #expect(childAsOpenBox() === childAsOpenBox())
    #expect(childAsOpenBox() is ChildBox)
    #expect((hiddenReader() as AnyObject) === (hiddenReader() as AnyObject))
    #expect(success() === success())
    #expect(isSharedFinal(box: finalBox()))

    // The value of a generic object keeps its identity too.
    #expect(finalBox().value === finalBox().value)

    // Another static type of the same Kotlin object is another Swift object. The two are equal.
    let star = finalBoxAsStar()
    #expect(star === finalBoxAsStar())
    #expect(star.isEqual(finalBox()))
    #expect(Set<NSObject>([star, finalBox()]).count == 1)
}

@Test
func objectMadeInSwiftKeepsItsIdentityThroughKotlin() throws {
    let made = FinalBox(value: Plain(id: 9))
    #expect(identity(value: made) === made)
    let madeOpen = OpenBox(value: Plain(id: 9))
    #expect(identity(value: madeOpen) === madeOpen)
    #expect(identity(value: [made, made])[1] === made)
}

@Test
func projectionOfOneObjectIsOneSwiftObject() throws {
    let plainOut = OutBox(value: Plain(id: 1))
    let anyOut = OutBox<KotlinBase>(plainOut.covariant)
    #expect(anyOut === OutBox<KotlinBase>(plainOut.covariant))
    #expect(anyOut.value === plainOut.value)

    let resource = success()
    guard case .success(let first) = resource.sealedType(), case .success(let second) = resource.sealedType() else {
        Issue.record("Expected a success")
        return
    }
    #expect(first.value === second.value)
}

@Test
func wrappersHoldUnderContention() throws {
    let failures = Counter()
    DispatchQueue.concurrentPerform(iterations: 8) { _ in
        for _ in 0..<2_000 {
            autoreleasepool {
                let first = finalBox()
                let second = finalBox()
                let reader = hiddenReader()
                if first !== second || first.value.id != 1 || reader.read().id != 4 || newFinalBox().value.id != 6 {
                    failures.increment()
                }
            }
        }
    }
    #expect(failures.value == 0)
}

@Test
func wrappersOfGenericObjectsAreFreed() throws {
    // A wrapper that is never freed costs 80 bytes. The limit is 8 bytes per crossing: the heap of a test process is not quiet.
    let crossings = 50_000
    let limit = crossings * 8

    #expect(heapGrowth(crossings) { _ = finalBox() } < limit, "the same object of a final generic class")
    #expect(heapGrowth(crossings) { _ = newFinalBox() } < limit, "a new object of a final generic class")
    #expect(heapGrowth(crossings) { _ = openBox() } < limit, "the same object of an open generic class")
    #expect(heapGrowth(crossings) { _ = newOpenBox() } < limit, "a new object of an open generic class")
    #expect(heapGrowth(crossings) { _ = hiddenReader() } < limit, "the same object behind a generic interface")
    #expect(heapGrowth(crossings) { _ = newHiddenReader() } < limit, "a new object behind a generic interface")
    #expect(heapGrowth(crossings) { _ = FinalBox(value: Int32(1)) } < limit, "an object of a generic class made in Swift")
    #expect(heapGrowth(crossings) { _ = success().sealedType() } < limit, "a case of a sealed generic class")
    #expect(heapGrowth(crossings / 100) { _ = boxes(count: 100) } < limit, "a list of objects of a generic class")

    let plainOut = OutBox(value: Plain(id: 1))
    #expect(heapGrowth(crossings) { _ = OutBox<KotlinBase>(plainOut.covariant) } < limit, "a covariant projection")
}

@Test
func freedWrapperIsMadeAgain() throws {
    weak var weakBox: FinalBox<Plain>?
    autoreleasepool {
        let box = newFinalBox()
        weakBox = box
        #expect(weakBox != nil)
        // None of these may keep the wrapper.
        #expect(box.value.id == 6)
        #expect(!isSharedFinal(box: box))
        #expect(box != newFinalBox())
        #expect(Set([box, box]).count == 1)
        #expect(identity(value: box) === box)
        #expect(identity(value: [box])[0] === box)
    }
    #expect(weakBox == nil, "nothing but Swift holds the wrapper of a new object")

    // The shared object lives on, and Swift gets a wrapper for it again.
    autoreleasepool { weakBox = finalBox() }
    #expect(finalBox().value.id == 1)
}

private final class Counter: @unchecked Sendable {
    private let lock = NSLock()
    private var count = 0

    func increment() {
        lock.lock()
        count += 1
        lock.unlock()
    }

    var value: Int {
        lock.lock()
        defer { lock.unlock() }
        return count
    }
}

private func heapInUse() -> Int {
    var statistics = malloc_statistics_t()
    malloc_zone_statistics(nil, &statistics)
    return Int(statistics.size_in_use)
}

// Lets Kotlin collect what the calls made, and lets the main queue run: wrappers can be released there.
private func settle() {
    for _ in 0..<3 {
        collect()
        RunLoop.main.run(until: Date(timeIntervalSinceNow: 0.05))
    }
}

private func heapGrowth(_ crossings: Int, _ body: () -> Void) -> Int {
    autoreleasepool { for _ in 0..<1_000 { body() } }
    settle()
    let before = heapInUse()
    var left = crossings
    while left > 0 {
        autoreleasepool { for _ in 0..<min(left, 1_000) { body() } }
        left -= 1_000
    }
    settle()
    return heapInUse() - before
}
