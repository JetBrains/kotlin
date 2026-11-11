import Foundation
import H3Generics
import KotlinRuntime
import KotlinRuntimeSupport
import Testing

private final class SwiftStringRepo: KotlinBase, Repository {
    var value: String
    init(_ initial: String) {
        self.value = initial
        super.init()
    }
    func get() -> String { value }
    func save(item: String) -> Bool {
        value = item
        return true
    }
}

private final class TaggedBox<T: _KotlinBridgeable>: Box<T> {
    let tag: String
    init(tag: String, value: T) {
        self.tag = tag
        super.init(value: value)
    }
    override func current() -> T {
        return super.value
    }
}

@Test
func patInterfacesAndNominalWrappers() throws {
    let hiddenRepo: any Repository<Int32> = makeHiddenIntRepo(initial: 10)
    let initialVal = hiddenRepo.get()
    #expect(initialVal == 10)
    let savedOk = hiddenRepo.save(item: 25)
    #expect(savedOk)
    let afterSaveVal = hiddenRepo.get()
    #expect(afterSaveVal == 25)

    // Nominal wrapper AnyRepository<Int32> nested inside generic class Box<AnyRepository<Int32>>
    let boxedRepo: Box<AnyRepository<Int32>> = wrapRepoInBox(repo: hiddenRepo)
    #expect(boxedRepo.value.get() == 25)
    #expect(syncRepo(repo: boxedRepo.value, next: 99) == 99)
    let syncedVal = hiddenRepo.get()
    #expect(syncedVal == 99)

    // Concrete and generic Kotlin conformers
    let ktIntRepo = IntRepo(v: 7)
    #expect(syncRepo(repo: ktIntRepo, next: 8) == 8)

    let ktBoxRepo = BoxRepo(v: "alpha")
    #expect(syncRepo(repo: ktBoxRepo, next: "beta") == "beta")
    #expect(ktBoxRepo.get() == "beta")

    // Specialized sub-protocol StringRepository : Repository
    let strRepo = MemoryStringRepo(s: "hello")
    #expect(strRepo.upper() == "HELLO")
    #expect(syncRepo(repo: strRepo, next: "world") == "world")
    #expect(strRepo.upper() == "WORLD")

    // Swift class conforming to PAT Repository passed into Kotlin
    let swiftRepo = SwiftStringRepo("init")
    #expect(syncRepo(repo: swiftRepo, next: "updated-from-kotlin") == "updated-from-kotlin")
    #expect(swiftRepo.value == "updated-from-kotlin")

    // Sibling AnyStream collision -> _AnyStream<T>
    let plainAnyStream = AnyStream(label: "sibling")
    #expect(plainAnyStream.label == "sibling")
    let typedStream = singleStream(v: Int32(77))
    let nextStreamItem = typedStream.nextItem()
    #expect(nextStreamItem == 77)
}

@Test
func zeroCostCompileTimeVarianceAndProjections() throws {
    let dog = Dog(name: "Rex", breed: "Husky")
    let dogOut = OutBox(value: dog)

    // Compile-time covariant upcast OutBox<Dog> -> OutBox<Animal>
    let animalOut = OutBox<Animal>(dogOut.covariant)
    #expect(animalOut.value.name == "Rex")
    #expect((animalOut.value as? Dog)?.breed == "Husky")

    // Compile-time contravariant projection InSink<Animal> -> InSink<Dog>
    let animalSink = InSink<Animal>(prefix: "vet")
    let dogSink = InSink<Dog>(animalSink.contravariant)
    #expect(dogSink.accept(item: dog).hasPrefix("vet:"))

    // PAT .covariant / .contravariant projections via AnySource / AnyConsumerSink
    let dogSource = SimpleSource(v: dog)
    let animalSource = AnySource<Animal>(dogSource.covariant)
    #expect((animalSource.fetch() as? Dog)?.breed == "Husky")

    let animalConsumer = AnimalNameSink()
    let dogConsumer = AnyConsumerSink<Dog>(animalConsumer.contravariant)
    #expect(dogConsumer.push(value: dog) == "animal:Rex")

    // Input use-site projection lifting (`Box<*>`, `Box<out Animal>`)
    let dogBox = Box(value: dog)
    #expect(describeStarBox(b: dogBox).hasPrefix("star:"))
    #expect(readOutAnimalName(b: dogBox) == "Rex")
}

@Test
func multiBoundsAndProtocolAndClassFBounds() throws {
    let td = TrainedDog(name: "Sky", title: "Champion", tricks: 12)
    #expect(describeMultiBound(x: td) == "Sky:Champion:12")

    let dual = DualEndpoint(endpoint: EchoEndpoint(buf: "boot"))
    #expect(dual.ping(msg: "hi") == "ack:hi->hi")

    let s1 = Score(points: 10)
    let s2 = Score(points: 25)
    #expect(maxOfTwo(a: s1, b: s2).points == 25)
    #expect(s2.isAtLeast(other: s1))
    #expect(!s1.isAtLeast(other: s2))

    let crtp = ConcreteSelfRef(tag: "ok")
    #expect(crtp.tag == "ok")
}

@Test
func frozenGenericSealedClassesAndInterfaces() throws {
    let dogState: UiState<Dog> = makeSuccessState(data: Dog(name: "Ace", breed: "Beagle"))
    let animalState = UiState<Animal>(dogState.covariant)

    switch animalState.sealedType() {
    case .loading:
        Issue.record("Expected success, got loading")
    case .success(let s):
        #expect(s.value.data.name == "Ace")
        #expect((s.value.data as? Dog)?.breed == "Beagle")
    case .failed(let f):
        Issue.record("Unexpected failed: \(f.value.reason)")
    }

    let loading: UiState<Int32> = makeLoadingState()
    switch loading.sealedType() {
    case .loading:
        break
    default:
        Issue.record("Expected loading")
    }

    let okRes: any ApiResult<String> = makeOkResult(payload: "done")
    switch okRes.sealedType() {
    case .empty:
        Issue.record("Expected ok")
    case .ok(let ok):
        #expect(ok.value.payload == "done")
    }

    let emptyRes: any ApiResult<Int32> = makeEmptyResult()
    switch emptyRes.sealedType() {
    case .empty:
        break
    default:
        Issue.record("Expected empty")
    }
}

@Test
func valueTypesSubclassesCompanionsAndZeroStableRefLeaks() throws {
    // Char (UTF16.CodeUnit) through generics and Any
    let ch: UTF16.CodeUnit = Array("K".utf16)[0]
    let charBox = Box(value: ch)
    #expect(charBox.value == ch)
    #expect(anyChar() as? UTF16.CodeUnit == Array("Z".utf16)[0])

    // KotlinUnit
    let unitBox = Box(value: KotlinUnit.shared)
    #expect(unitBox.value == KotlinUnit.shared)

    // Set<K> & Dictionary<K, V> with inferred Hashable
    #expect(singletonSet(k: Int32(7)) == Set([7]))
    #expect(singletonMap(k: "k", v: Int32(9)) == ["k": 9])

    // Dynamic Kotlin subclass identity through open generic Box<Animal>
    let animalBox = makeDogInAnimalBox()
    #expect((animalBox.value as? Dog)?.breed == "Shepherd")

    // Swift generic subclass specialization preservation
    let tagged = TaggedBox(tag: "t1", value: Int32(42))
    #expect(tagged.current() == 42)
    let echoed = idValue(x: tagged as Box<Int32>)
    #expect((echoed as? TaggedBox<Int32>) === tagged)
    #expect((echoed as? TaggedBox<Int32>)?.tag == "t1")

    // Hoisted companion & nested class
    #expect(Box<Int32>.Companion.shared.tag() == "BoxCompanion")
    #expect(Box<Int32>.Meta(label: "meta").label == "meta")

    // Warm up once, then verify 0 StableRef leaks across 500 iterations
    _ = idValue(x: Int32(1))
    _ = takeAnyValue(x: Int32(1))
    _ = echoMode(m: .FAST)
    let beforeRefs = currentStableRefs()
    try #require(beforeRefs >= 0)

    autoreleasepool {
        for i: Int32 in 0..<500 {
            let b = Box(value: i)
            let mapped = mapBox(box: b) { $0 + 1 }
            #expect(mapped.value == i + 1)
            #expect(idValue(x: "v\(i)") == "v\(i)")
            #expect(takeAnyValue(x: i) == "\(i)")
            #expect(takeNullableAnyValue(x: Optional<Int32>.none) == "null")
            #expect(echoMode(m: .SAFE) == .SAFE)
        }
    }

    let afterRefs = currentStableRefs()
    #expect(afterRefs - beforeRefs == 0, "Leaked \(afterRefs - beforeRefs) StableRefs!")
}
