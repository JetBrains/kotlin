import CrossLangGenericInterfaces
import Foundation
import KotlinRuntime
import KotlinRuntimeSupport
import Testing

// ============================================================================
// Swift conformers & subclasses
// ============================================================================

// 1. Generic Swift class implementing generic Kotlin PAT `Repository<T>`
final class SwiftMemoryRepo<T: _KotlinBridgeable>: KotlinBase, Repository {
    var stored: T
    var saveCalls: Int = 0

    init(_ initial: T) {
        self.stored = initial
        super.init()
    }

    func get() -> T {
        return stored
    }

    func save(item: T) -> Bool {
        stored = item
        saveCalls += 1
        return true
    }
}

// 2a. Concrete Swift class implementing 2-param PAT `KeyValueStore<String, UserProfile>`
final class SwiftUserCache: KotlinBase, KeyValueStore {
    let storeTag: String
    var size: Int32 = 0
    var lastValue: UserProfile? = nil
    private var entries: [String: UserProfile] = [:]

    init(tag: String) {
        self.storeTag = tag
        super.init()
    }

    func get(key: String) -> UserProfile? {
        return entries[key]
    }

    func put(key: String, value: UserProfile) {
        entries[key] = value
        size = Int32(entries.count)
        lastValue = value
    }
}

// 2b. Generic Swift class implementing 2-param PAT `KeyValueStore<K, V>`
final class SwiftDictStore<K: _KotlinBridgeable & Hashable, V: _KotlinBridgeable>: KotlinBase, KeyValueStore {
    let storeTag: String
    var size: Int32 = 0
    var lastValue: V? = nil
    var backing: [K: V] = [:]

    init(tag: String) {
        self.storeTag = tag
        super.init()
    }

    func get(key: K) -> V? {
        return backing[key]
    }

    func put(key: K, value: V) {
        backing[key] = value
        size = Int32(backing.count)
        lastValue = value
    }
}

// 3a. Swift generic class overriding ONLY `raw`, inheriting Kotlin's default `format` & `tag`
final class SwiftDefaultFormatter<T: _KotlinBridgeable>: KotlinBase, CrossLangGenericInterfaces.Formatter {
    private let mapper: (T) -> T

    init(_ mapper: @escaping (T) -> T) {
        self.mapper = mapper
        super.init()
    }

    func raw(x: T) -> T {
        return mapper(x)
    }
}

// 3b. Swift generic class overriding `raw`, `format`, AND `tag`
final class SwiftCustomFormatter<T: _KotlinBridgeable>: KotlinBase, CrossLangGenericInterfaces.Formatter {
    let tag: String
    private let mapper: (T) -> T
    private let renderer: (T) -> String

    init(tag: String, mapper: @escaping (T) -> T, renderer: @escaping (T) -> String) {
        self.tag = tag
        self.mapper = mapper
        self.renderer = renderer
        super.init()
    }

    func raw(x: T) -> T {
        return mapper(x)
    }

    func format(x: T) -> String {
        return renderer(raw(x: x))
    }
}

// 4. Error types & Swift generic `Validator<T>` conformer
enum SwiftValidationError: Error, CustomStringConvertible, Equatable {
    case rejected(String)

    var description: String {
        switch self {
        case .rejected(let reason):
            return "swift-rejected:\(reason)"
        }
    }
}

final class SwiftPredicateValidator<T: _KotlinBridgeable>: KotlinBase, Validator {
    private let check: (T) throws -> T

    init(_ check: @escaping (T) throws -> T) {
        self.check = check
        super.init()
    }

    func validate(item: T) throws -> T {
        return try check(item)
    }
}

// 5a. Generic Swift conformer of refined `ReadWriteRepo<T> : Readable<T>, Writable<T>`
final class SwiftReadWriteMemory<T: _KotlinBridgeable>: KotlinBase, ReadWriteRepo {
    var value: T
    var history: [T] = []

    init(_ initial: T) {
        self.value = initial
        super.init()
    }

    func read() -> T {
        return value
    }

    func write(item: T) -> Bool {
        value = item
        history.append(item)
        return true
    }

    func swap(next: T) -> T {
        let old = value
        value = next
        history.append(next)
        return old
    }
}

// 5b. Concrete Swift conformer of specialized `StringReadWriteRepo : ReadWriteRepo<String>`
final class SwiftStringReadWriteMemory: KotlinBase, StringReadWriteRepo {
    var value: String

    init(_ initial: String) {
        self.value = initial
        super.init()
    }

    func read() -> String {
        return value
    }

    func write(item: String) -> Bool {
        value = item
        return true
    }

    func swap(next: String) -> String {
        let old = value
        value = next
        return old
    }

    func upper() -> String {
        return value.uppercased()
    }
}

// 5c. Single Swift class conforming to two independent generic PATs (`Readable<Int32>` & `Writable<String>`)
final class SwiftIntReaderStringWriter: KotlinBase, Readable, Writable {
    var currentInt: Int32
    var writtenStrings: [String] = []

    init(_ initial: Int32) {
        self.currentInt = initial
        super.init()
    }

    func read() -> Int32 {
        return currentInt
    }

    func write(item: String) -> Bool {
        writtenStrings.append(item)
        return true
    }
}

// 5d. Generic Swift conformer of multi-bound PAT `TaggedShelter<T> where T : Animal, T : PetTag`
final class SwiftTaggedShelter<T: Animal & PetTag>: KotlinBase, TaggedShelter {
    var resident: T

    init(_ initial: T) {
        self.resident = initial
        super.init()
    }

    func current() -> T {
        return resident
    }

    func accept(pet: T) -> String {
        resident = pet
        return "accepted:\(pet.name):\(pet.badge)"
    }
}

// 6a. Generic Swift subclass of `open class BaseRepo<T> : Repository<T>` calling `super`
final class AuditedRepo<T: _KotlinBridgeable>: BaseRepo<T> {
    var auditLog: [T] = []
    var stateSetCount: Int = 0
    var getCount: Int = 0

    init(initial: T) {
        super.init(state: initial)
    }

    override var state: T {
        get {
            return super.state
        }
        set {
            stateSetCount += 1
            super.state = newValue
        }
    }

    override func get() -> T {
        getCount += 1
        return super.get()
    }

    override func save(item: T) -> Bool {
        auditLog.append(item)
        return super.save(item: item)
    }
}

// 6b. Concrete Swift subclass of `open class BaseRepo<String> : Repository<String>` calling `super`
final class BracketedStringRepo: BaseRepo<String> {
    let prefix: String

    init(prefix: String, initial: String) {
        self.prefix = prefix
        super.init(state: initial)
    }

    override func get() -> String {
        return "\(prefix):\(super.get())"
    }

    override func save(item: String) -> Bool {
        return super.save(item: "[\(item)]")
    }

    override func summary() -> String {
        return "swift-\(super.summary())"
    }
}

// 7. Generic Swift conformer of `Transformer<T>` with generic method `func mapCurrent<R>(fn: (T) -> R) -> R`
final class SwiftTransformer<T: _KotlinBridgeable>: KotlinBase, Transformer {
    var currentValue: T
    var mapCallCount: Int = 0
    var unitResultCount: Int = 0

    init(_ initial: T) {
        self.currentValue = initial
        super.init()
    }

    func mapCurrent<R: _KotlinBridgeable>(fn: @escaping (T) -> R) -> R {
        mapCallCount += 1
        let out = fn(currentValue)
        if let boxed = out as? KotlinAny, boxed.value is KotlinBase {
            unitResultCount += 1
        } else if out is KotlinUnit {
            unitResultCount += 1
        }
        return out
    }
}

// ============================================================================
// E2E Execution Tests
// ============================================================================

@Test
func genericSwiftClassImplementingKotlinPatAcrossAllTypes() throws {
    // 1. Int32
    let intRepo = SwiftMemoryRepo<Int32>(10)
    #expect(mutateIntRepo(repo: intRepo, delta: 5) == 15)
    #expect(intRepo.stored == 15)
    #expect(intRepo.saveCalls == 1)
    #expect(syncRepo(repo: intRepo, next: 42) == 42)
    #expect(readRepo(repo: intRepo) == 42)
    #expect(intRepo.stored == 42)
    #expect(intRepo.saveCalls == 2)

    let echoedIntRepo = echoRepo(repo: intRepo)
    #expect((echoedIntRepo as? SwiftMemoryRepo<Int32>) === intRepo)
    #expect((echoAnyObject(x: intRepo) as? SwiftMemoryRepo<Int32>) === intRepo)
    #expect(echoAnyObject(x: intRepo) as? SwiftMemoryRepo<String> == nil)
    #expect(echoAnyObject(x: intRepo) as? SwiftMemoryRepo<Dog> == nil)

    let intRegistry = RepoRegistry<Int32>()
    let ktIntRepo = KotlinMemoryRepo<Int32>(current: 100)
    intRegistry.register(repo: intRepo)
    intRegistry.register(repo: ktIntRepo)
    let snapInt = intRegistry.snapshot()
    #expect(snapInt.count == 2)
    #expect(readAllFromRepoList(repos: snapInt) == [42, 100])
    #expect(intRegistry.broadcastSave(value: 77) == 2)
    #expect(intRepo.stored == 77)
    #expect(ktIntRepo.get() == 77)
    #expect(readAllFromRepoList(repos: echoRepoList(repos: snapInt)) == [77, 77])
    #expect((intRegistry.at(index: 0) as? SwiftMemoryRepo<Int32>) === intRepo)

    // 2. String
    let strRepo = SwiftMemoryRepo<String>("hello")
    #expect(mutateStringRepo(repo: strRepo, suffix: "-world") == "hello-world")
    #expect(syncRepo(repo: strRepo, next: "kotlin") == "kotlin")
    #expect(strRepo.stored == "kotlin")
    #expect((echoRepo(repo: strRepo) as? SwiftMemoryRepo<String>) === strRepo)
    #expect(echoAnyObject(x: strRepo) as? SwiftMemoryRepo<Int32> == nil)

    let strRegistry = RepoRegistry<String>()
    strRegistry.register(repo: strRepo)
    #expect(readAllFromRepoList(repos: strRegistry.snapshot()) == ["kotlin"])
    #expect((strRegistry.at(index: 0) as? SwiftMemoryRepo<String>) === strRepo)

    // 3. Optional<String>
    let optRepo = SwiftMemoryRepo<String?>(nil)
    #expect(readRepo(repo: optRepo) == nil)
    #expect(mutateOptionalStringRepo(repo: optRepo, fallback: "filled") == "filled")
    #expect(optRepo.stored == "filled")
    #expect(mutateOptionalStringRepo(repo: optRepo, fallback: "unused") == nil)
    #expect(optRepo.stored == nil)
    #expect(syncRepo(repo: optRepo, next: "explicit") == "explicit")
    #expect(syncRepo(repo: optRepo, next: nil) == nil)
    #expect((echoRepo(repo: optRepo) as? SwiftMemoryRepo<String?>) === optRepo)
    #expect(echoAnyObject(x: optRepo) as? SwiftMemoryRepo<String> == nil)

    let optRegistry = RepoRegistry<String?>()
    optRegistry.register(repo: optRepo)
    optRegistry.register(repo: KotlinMemoryRepo<String?>(current: "present"))
    #expect(readAllFromRepoList(repos: optRegistry.snapshot()) == [nil, "present"])
    #expect((optRegistry.at(index: 0) as? SwiftMemoryRepo<String?>) === optRepo)

    // 4. [Int32]
    let listRepo = SwiftMemoryRepo<[Int32]>([1, 2])
    #expect(mutateIntListRepo(repo: listRepo, extra: 3) == [1, 2, 3])
    #expect(listRepo.stored == [1, 2, 3])
    #expect(syncRepo(repo: listRepo, next: [9, 8]) == [9, 8])
    #expect((echoRepo(repo: listRepo) as? SwiftMemoryRepo<[Int32]>) === listRepo)
    #expect(echoAnyObject(x: listRepo) as? SwiftMemoryRepo<[String]> == nil)

    let listRegistry = RepoRegistry<[Int32]>()
    listRegistry.register(repo: listRepo)
    #expect(readAllFromRepoList(repos: listRegistry.snapshot()) == [[9, 8]])
    #expect((listRegistry.at(index: 0) as? SwiftMemoryRepo<[Int32]>) === listRepo)

    // 5. Kotlin open class Dog : Animal
    let initialDog = Dog(name: "Rex", breed: "Shepherd")
    let dogRepo = SwiftMemoryRepo<Dog>(initialDog)
    let renamedDog = mutateDogRepo(repo: dogRepo, newName: "Bolt")
    #expect(renamedDog.name == "Bolt")
    #expect(renamedDog.breed == "Shepherd")
    #expect(dogRepo.stored === renamedDog)
    let nextDog = Dog(name: "Ace", breed: "Collie")
    #expect(syncRepo(repo: dogRepo, next: nextDog) === nextDog)
    #expect((echoRepo(repo: dogRepo) as? SwiftMemoryRepo<Dog>) === dogRepo)
    #expect(echoAnyObject(x: dogRepo) as? SwiftMemoryRepo<Animal> == nil)

    let dogRegistry = RepoRegistry<Dog>()
    dogRegistry.register(repo: dogRepo)
    let dogsFromList = readAllFromRepoList(repos: dogRegistry.snapshot())
    #expect(dogsFromList.count == 1)
    #expect(dogsFromList[0] === nextDog)
    #expect((dogRegistry.at(index: 0) as? SwiftMemoryRepo<Dog>) === dogRepo)

    // 6. Kotlin enum Priority
    let enumRepo = SwiftMemoryRepo<Priority>(.LOW)
    #expect(mutatePriorityRepo(repo: enumRepo) == .MEDIUM)
    #expect(enumRepo.stored == .MEDIUM)
    #expect(syncRepo(repo: enumRepo, next: .HIGH) == .HIGH)
    #expect(enumRepo.stored == .HIGH)
    #expect((echoRepo(repo: enumRepo) as? SwiftMemoryRepo<Priority>) === enumRepo)
    #expect(echoAnyObject(x: enumRepo) as? SwiftMemoryRepo<Int32> == nil)

    let enumRegistry = RepoRegistry<Priority>()
    enumRegistry.register(repo: enumRepo)
    enumRegistry.register(repo: KotlinMemoryRepo<Priority>(current: .LOW))
    #expect(readAllFromRepoList(repos: enumRegistry.snapshot()) == [.HIGH, .LOW])
    #expect((enumRegistry.at(index: 0) as? SwiftMemoryRepo<Priority>) === enumRepo)
}

@Test
func multiParameterKeyValueStoreInSwift() throws {
    // Concrete SwiftUserCache: KotlinBase, KeyValueStore
    let userCache = SwiftUserCache(tag: "users")
    #expect(readStoreKey(store: userCache, key: "u1") == nil)
    let summary = exerciseConcreteUserStore(store: userCache)
    #expect(summary == "users|2|ada|none|grace")
    #expect(userCache.size == 2)
    #expect(userCache.lastValue?.handle == "grace")

    let alan = UserProfile(id: 3, handle: "alan")
    #expect(putAndReadStore(store: userCache, key: "u3", value: alan) === alan)
    #expect(userCache.size == 3)
    #expect(overwriteStoreTracking(store: userCache, newSize: 99, newLast: nil) == nil)
    #expect(userCache.size == 99)
    #expect(userCache.lastValue == nil)
    #expect((echoKeyValueStore(store: userCache) as? SwiftUserCache) === userCache)

    // Generic SwiftDictStore<K, V> with (String, Int32)
    let scores = SwiftDictStore<String, Int32>(tag: "scores")
    #expect(readStoreKey(store: scores, key: "alice") == nil)
    #expect(putAndReadStore(store: scores, key: "alice", value: 95) == 95)
    #expect(putAndReadStore(store: scores, key: "bob", value: 88) == 88)
    #expect(scores.backing == ["alice": 95, "bob": 88])
    #expect(scores.size == 2)
    #expect(scores.lastValue == 88)
    #expect(describeStoreState(store: scores) == "scores:2:88")

    #expect(overwriteStoreTracking(store: scores, newSize: 10, newLast: 100) == 100)
    #expect(scores.size == 10)
    #expect(scores.lastValue == 100)
    #expect(overwriteStoreTracking(store: scores, newSize: 2, newLast: nil) == nil)
    #expect(scores.lastValue == nil)
    #expect((echoKeyValueStore(store: scores) as? SwiftDictStore<String, Int32>) === scores)
    #expect(echoAnyObject(x: scores) as? SwiftDictStore<Int32, String> == nil)

    // Generic SwiftDictStore<K, V> with (Priority, [Int32])
    let buckets = SwiftDictStore<Priority, [Int32]>(tag: "buckets")
    #expect(putAndReadStore(store: buckets, key: .HIGH, value: [10, 20]) == [10, 20])
    #expect(readStoreKey(store: buckets, key: .LOW) == nil)
    #expect(buckets.lastValue == [10, 20])
}

@Test
func kotlinGenericInterfaceDefaultsVsSwiftOverrides() throws {
    // 1. Swift class overriding ONLY `raw`, inheriting Kotlin's default `format` and `tag`
    let defaultIntFmt = SwiftDefaultFormatter<Int32> { $0 + 1 }
    // Direct Swift calls to inherited Kotlin defaults (whose internal `raw(x)` self-call reaches Swift!)
    #expect(defaultIntFmt.raw(x: 9) == 10)
    #expect(defaultIntFmt.tag == "default")
    #expect(defaultIntFmt.format(x: 9) == "fmt:10")
    // Existential Swift dispatch
    let existentialDefault: any CrossLangGenericInterfaces.Formatter<Int32> = defaultIntFmt
    #expect(existentialDefault.tag == "default")
    #expect(existentialDefault.format(x: 20) == "fmt:21")
    // Kotlin-side generic and concrete dispatch (must not infinitely recurse through patched itable!)
    #expect(formatFromKotlin(formatter: defaultIntFmt, item: 41) == "default|fmt:42")
    #expect(formatIntFromKotlin(formatter: defaultIntFmt, item: 41) == "default|fmt:42")
    #expect(rawFromKotlin(formatter: defaultIntFmt, item: 41) == 42)

    let defaultStrFmt = SwiftDefaultFormatter<String> { $0.uppercased() }
    #expect(defaultStrFmt.format(x: "abc") == "fmt:ABC")
    #expect(formatFromKotlin(formatter: defaultStrFmt, item: "abc") == "default|fmt:ABC")

    // 2. Swift class overriding `raw`, `format`, AND `tag`
    let customIntFmt = SwiftCustomFormatter<Int32>(
        tag: "custom-int",
        mapper: { $0 * 2 },
        renderer: { "custom<\($0)>" }
    )
    #expect(customIntFmt.tag == "custom-int")
    #expect(customIntFmt.raw(x: 6) == 12)
    #expect(customIntFmt.format(x: 6) == "custom<12>")
    #expect(formatFromKotlin(formatter: customIntFmt, item: 6) == "custom-int|custom<12>")
    #expect(formatIntFromKotlin(formatter: customIntFmt, item: 6) == "custom-int|custom<12>")

    // 3. Pure Kotlin Formatter<T> implementation for comparison
    let ktFmt = KotlinFormatter<String> { "[\($0)]" }
    #expect(ktFmt.tag == "default")
    #expect(ktFmt.format(x: "ok") == "fmt:[ok]")
    #expect(formatFromKotlin(formatter: ktFmt, item: "ok") == "default|fmt:[ok]")
}

@Test
func throwsErrorPropagationAcrossGenericPatReverseBridges() throws {
    // 1. Generic Swift Validator<Int32>: valid round-trip + Swift error caught in Kotlin + relayed to Swift
    let swiftIntVal = SwiftPredicateValidator<Int32> { x in
        if x < 0 {
            throw SwiftValidationError.rejected("neg:\(x)")
        }
        return x + 100
    }
    #expect(try swiftIntVal.validate(item: 5) == 105)
    #expect(validateCatchingInKotlin(validator: swiftIntVal, item: 5) == "ok:105")
    #expect(try relayValidateThroughKotlin(validator: swiftIntVal, item: 7) == 107)

    let caughtInKotlin = validateCatchingInKotlin(validator: swiftIntVal, item: -3)
    #expect(caughtInKotlin.hasPrefix("swift-err:"))
    #expect(caughtInKotlin.contains("neg:-3"))

    // Swift error relayed through Kotlin `@Throws` function unwraps back to the exact SwiftValidationError
    do {
        _ = try relayValidateThroughKotlin(validator: swiftIntVal, item: -9)
        Issue.record("Expected relayValidateThroughKotlin to throw SwiftValidationError")
    } catch let err as SwiftValidationError {
        #expect(err == .rejected("neg:-9"))
    }

    // 2. Swift Validator<String> throwing exported Kotlin ValidationKotlinException
    let swiftStrVal = SwiftPredicateValidator<String> { s in
        if s.isEmpty {
            throw ValidationKotlinException(message: "empty-from-swift")
        }
        return "valid:\(s)"
    }
    #expect(validateCatchingInKotlin(validator: swiftStrVal, item: "hi") == "ok:valid:hi")
    #expect(validateCatchingInKotlin(validator: swiftStrVal, item: "") == "kt-exc:empty-from-swift")

    do {
        _ = try relayValidateThroughKotlin(validator: swiftStrVal, item: "")
        Issue.record("Expected ValidationKotlinException")
    } catch let ktErr as ValidationKotlinException {
        #expect(ktErr.message == "empty-from-swift")
    }

    // 3. Kotlin-thrown error caught in Swift via `any Validator<T>`
    let ktIntVal: any Validator<Int32> = makeKotlinPositiveIntValidator()
    #expect(try ktIntVal.validate(item: 4) == 40)
    do {
        _ = try ktIntVal.validate(item: 0)
        Issue.record("Expected ktIntVal.validate(0) to throw")
    } catch let ktErr as ValidationKotlinException {
        #expect(ktErr.message == "non-positive:0")
    }

    let ktStrVal: any Validator<String> = makeKotlinRejectingValidator(forbidden: "poison")
    #expect(try ktStrVal.validate(item: "safe") == "safe")
    do {
        _ = try ktStrVal.validate(item: "poison")
        Issue.record("Expected ktStrVal.validate(poison) to throw")
    } catch let ktErr as ValidationKotlinException {
        #expect(ktErr.message == "forbidden:poison")
    }
}

@Test
func subInterfaceRefinementAndMultiPatConformances() throws {
    // 1. Generic `ReadWriteRepo<T> : Readable<T>, Writable<T>` implemented in Swift
    let intRW = SwiftReadWriteMemory<Int32>(1)
    let traceInt = exerciseReadWriteRepo(rw: intRW, first: 10, second: 20)
    #expect(traceInt == [1, 10, 10, 20])
    #expect(intRW.value == 20)
    #expect(intRW.history == [10, 20])
    #expect(pullReadable(source: intRW) == 20)
    #expect(pushWritable(sink: intRW, item: 30))
    #expect(intRW.read() == 30)

    // 2. Specialized `StringReadWriteRepo : ReadWriteRepo<String>` implemented in Swift
    let strRW = SwiftStringReadWriteMemory("boot")
    let summary = exerciseStringReadWriteRepo(srw: strRW, first: "mid", second: "done")
    #expect(summary == "boot,mid,mid,done|upper=DONE")
    #expect(pullReadable(source: strRW) == "done")
    #expect(pushWritable(sink: strRW, item: "final"))
    #expect(strRW.upper() == "FINAL")

    // 3. Single Swift class conforming to two independent generic PATs (`Readable<Int32>` & `Writable<String>`)
    let dual = SwiftIntReaderStringWriter(77)
    let asReadable: any Readable<Int32> = dual
    let asWritable: any Writable<String> = dual
    #expect(asReadable.read() == 77)
    #expect(asWritable.write(item: "direct") == true)
    #expect(pullReadable(source: dual) == 77)
    #expect(pushWritable(sink: dual, item: "via-generic"))
    #expect(bridgeSeparateReadableWritable(r: dual, w: dual))
    #expect(relayMultiPatConformer(dual: dual))
    #expect(dual.writtenStrings == ["direct", "via-generic", "sep:77", "multi:77"])

    // 4. Multi-bound generic PAT (`TaggedShelter<T> where T : Animal, T : PetTag`) with overloaded methods
    let td1 = TaggedDog(name: "Scout", breed: "Beagle", badge: "K9-1")
    let td2 = TaggedDog(name: "Nova", breed: "Husky", badge: "K9-2")
    let shelter = SwiftTaggedShelter<TaggedDog>(td1)
    #expect(shelter.accept(fallbackLabel: "visitor") == "fallback:visitor")
    let shelterTrace = exerciseTaggedShelter(shelter: shelter, next: td2)
    #expect(shelterTrace == "K9-1|accepted:Nova:K9-2|fallback:guest|tagged:Nova:Husky:K9-2")
    #expect(shelter.resident === td2)
}

@Test
func swiftSubclassesOfOpenGenericKotlinClassImplementingPat() throws {
    // 1. Generic `AuditedRepo<T>: BaseRepo<T>` overriding PAT methods and calling `super`
    let auditedInt = AuditedRepo<Int32>(initial: 5)
    #expect(auditedInt.get() == 5)
    #expect(auditedInt.getCount == 1)

    // Mutate via `BaseRepo<T>` parameter in Kotlin (exercises class reverse bridge + inherited non-overridden `summary()`)
    let baseResult = mutateViaBaseRepo(repo: auditedInt, next: 12)
    #expect(baseResult == "12|base(12)#1")
    #expect(auditedInt.auditLog == [12])
    #expect(auditedInt.baseSaveCount == 1)
    #expect(auditedInt.state == 12)

    // Mutate via `Repository<T>` PAT parameter in Kotlin (exercises interface dispatch -> Swift override -> super)
    #expect(syncRepo(repo: auditedInt, next: 25) == 25)
    #expect(auditedInt.auditLog == [12, 25])
    #expect(auditedInt.baseSaveCount == 2)
    #expect(auditedInt.state == 25)

    // Mutate overridden `open var state: T` from Kotlin (exercises generic property setter reverse bridge -> super.state)
    let prevSets = auditedInt.stateSetCount
    #expect(assignBaseRepoStateInKotlin(repo: auditedInt, next: 99) == 99)
    #expect(auditedInt.stateSetCount == prevSets + 1)
    #expect(auditedInt.state == 99)

    // Identity & generic specialization safety across both `BaseRepo<T>` and `Repository<T>`
    #expect((echoBaseRepo(repo: auditedInt) as? AuditedRepo<Int32>) === auditedInt)
    #expect((echoRepo(repo: auditedInt) as? AuditedRepo<Int32>) === auditedInt)
    #expect(echoAnyObject(x: auditedInt) as? AuditedRepo<String> == nil)

    // Generic `AuditedRepo<Dog>`
    let d1 = Dog(name: "Max", breed: "Pug")
    let d2 = Dog(name: "Leo", breed: "Boxer")
    let auditedDog = AuditedRepo<Dog>(initial: d1)
    #expect(syncRepo(repo: auditedDog, next: d2) === d2)
    #expect(auditedDog.auditLog.count == 1)
    #expect(auditedDog.auditLog[0] === d2)
    #expect((echoRepo(repo: auditedDog) as? AuditedRepo<Dog>) === auditedDog)
    #expect(echoAnyObject(x: auditedDog) as? AuditedRepo<Animal> == nil)

    // 2. Concrete `BracketedStringRepo: BaseRepo<String>` overriding PAT methods + `summary()` and calling `super`
    let bracketed = BracketedStringRepo(prefix: "tag", initial: "init")
    #expect(bracketed.get() == "tag:init")
    #expect(mutateViaBaseRepo(repo: bracketed, next: "one") == "tag:[one]|swift-base([one])#1")
    #expect(syncRepo(repo: bracketed, next: "two") == "tag:[two]")
    #expect(bracketed.state == "[two]")
    #expect(bracketed.baseSaveCount == 2)
    #expect((echoRepo(repo: bracketed) as? BracketedStringRepo) === bracketed)
}

@Test
func genericMethodsInsideGenericPatAndZeroStableRefLeaks() throws {
    // 1. Kotlin implementation of `Transformer<T>` invoked from Swift & Kotlin
    let ktTrans = KotlinTransformer<Int32>(currentValue: 7)
    #expect(ktTrans.mapCurrent { "s:\($0 * 2)" } == "s:14")
    #expect(ktTrans.mapCurrent { [$0, $0 + 1] } == [7, 8])
    #expect(mapIntTransformerToLabelInKotlin(transformer: ktTrans) == "mapped:35")

    let hiddenTrans: any Transformer<String> = makeHiddenTransformer(initial: "kotlin")
    #expect(hiddenTrans.mapCurrent { Int32($0.count) } == 6)
    let ktMappedDog = mapTransformerToDogInKotlin(transformer: hiddenTrans, breed: "Corgi")
    #expect(ktMappedDog.name == "dog-kotlin")
    #expect(ktMappedDog.breed == "Corgi")

    // 2. Swift implementation of `Transformer<T>` invoked from BOTH Swift and Kotlin!
    let swiftIntTrans = SwiftTransformer<Int32>(9)
    #expect(swiftIntTrans.mapCurrent { "sw:\($0 + 1)" } == "sw:10")
    #expect(swiftIntTrans.mapCallCount == 1)
    #expect(mapIntTransformerToLabelInKotlin(transformer: swiftIntTrans) == "mapped:45")
    #expect(swiftIntTrans.mapCallCount == 2)
    #expect(runMapCurrentInKotlin(transformer: swiftIntTrans) { v in [v, v * 10] } == [9, 90])
    #expect(swiftIntTrans.mapCallCount == 3)

    let swiftStrTrans = SwiftTransformer<String>("swift")
    let dogFromSwiftTrans = mapTransformerToDogInKotlin(transformer: swiftStrTrans, breed: "Akita")
    #expect(dogFromSwiftTrans.name == "dog-swift")
    #expect(dogFromSwiftTrans.breed == "Akita")
    #expect(runMapCurrentInKotlin(transformer: swiftStrTrans) { s in Optional<Int32>.some(Int32(s.count)) } == 5)
    #expect(runMapCurrentInKotlin(transformer: swiftStrTrans) { _ in Optional<Int32>.none } == nil)

    // Kotlin `Unit`-returning lambda passed into Swift generic `Transformer<T>.mapCurrent<R>` + `KotlinUnit` specialization
    #expect(echoAnyObject(x: KotlinUnit.shared) is KotlinBase)
    let unitRepo = SwiftMemoryRepo<KotlinUnit>(KotlinUnit.shared)
    #expect(syncRepo(repo: unitRepo, next: KotlinUnit.shared) == KotlinUnit.shared)
    #expect(swiftIntTrans.mapCurrent { _ in KotlinUnit.shared } == KotlinUnit.shared)
    let intCollector = IntCollector()
    collectViaUnitMapCurrentInKotlin(transformer: swiftIntTrans, collector: intCollector)
    #expect(intCollector.snapshot() == [27])
    #expect(swiftIntTrans.unitResultCount == 2)

    // 3. Warm up all reverse/forward bridges once, then assert 0 StableRef leaks across 200 iterations
    let warmRepo = SwiftMemoryRepo<Int32>(0)
    _ = syncRepo(repo: warmRepo, next: 1)
    let warmStore = SwiftDictStore<String, Int32>(tag: "w")
    _ = putAndReadStore(store: warmStore, key: "k", value: 1)
    let warmFmt = SwiftDefaultFormatter<Int32> { $0 + 1 }
    _ = formatFromKotlin(formatter: warmFmt, item: 1)
    let warmVal = SwiftPredicateValidator<Int32> { x in
        if x < 0 { throw SwiftValidationError.rejected("w") }
        return x
    }
    _ = validateCatchingInKotlin(validator: warmVal, item: 1)
    _ = validateCatchingInKotlin(validator: warmVal, item: -1)
    _ = try? relayValidateThroughKotlin(validator: warmVal, item: -1)
    let warmKtVal = makeKotlinPositiveIntValidator()
    _ = try? warmKtVal.validate(item: 0)
    let warmAudited = AuditedRepo<Int32>(initial: 0)
    _ = syncRepo(repo: warmAudited, next: 1)
    let warmTrans = SwiftTransformer<Int32>(1)
    _ = mapIntTransformerToLabelInKotlin(transformer: warmTrans)
    let warmCollector = IntCollector()
    collectViaUnitMapCurrentInKotlin(transformer: warmTrans, collector: warmCollector)

    let beforeRefs = currentStableRefs()
    try #require(beforeRefs >= 0)

    autoreleasepool {
        for i: Int32 in 0..<200 {
            let repo = SwiftMemoryRepo<Int32>(i)
            #expect(syncRepo(repo: repo, next: i + 1) == i + 1)
            let reg = RepoRegistry<Int32>()
            reg.register(repo: repo)
            #expect(readAllFromRepoList(repos: reg.snapshot()) == [i + 1])

            let dogRepo = SwiftMemoryRepo<Dog>(Dog(name: "d\(i)", breed: "b"))
            #expect(mutateDogRepo(repo: dogRepo, newName: "n\(i)").name == "n\(i)")

            let store = SwiftDictStore<String, Int32>(tag: "s")
            #expect(putAndReadStore(store: store, key: "k\(i)", value: i) == i)

            let fmt = SwiftDefaultFormatter<Int32> { $0 + 2 }
            #expect(formatFromKotlin(formatter: fmt, item: i) == "default|fmt:\(i + 2)")

            let val = SwiftPredicateValidator<Int32> { x in
                if x < 0 { throw SwiftValidationError.rejected("neg") }
                return x + 1
            }
            #expect(validateCatchingInKotlin(validator: val, item: i) == "ok:\(i + 1)")
            #expect(validateCatchingInKotlin(validator: val, item: -1).contains("neg"))
            _ = try? relayValidateThroughKotlin(validator: val, item: -1)
            let ktVal = makeKotlinPositiveIntValidator()
            _ = try? ktVal.validate(item: 0)

            let audited = AuditedRepo<Int32>(initial: i)
            #expect(syncRepo(repo: audited, next: i + 3) == i + 3)

            let trans = SwiftTransformer<Int32>(i)
            #expect(mapIntTransformerToLabelInKotlin(transformer: trans) == "mapped:\(i * 5)")
            #expect(runMapCurrentInKotlin(transformer: trans) { $0 + 10 } == i + 10)
            let loopCollector = IntCollector()
            collectViaUnitMapCurrentInKotlin(transformer: trans, collector: loopCollector)
        }
    }

    let afterRefs = currentStableRefs()
    #expect(afterRefs - beforeRefs == 0, "Leaked \(afterRefs - beforeRefs) StableRefs across cross-language PAT loops!")
}
