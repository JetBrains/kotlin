import Foundation
import H3AsyncPatAndFlows
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import Testing

private struct SwiftRepoError: Error, Equatable, CustomStringConvertible {
    let code: Int
    let label: String
    var description: String { "SwiftRepoError(\(code):\(label))" }
}

private final class SwiftAsyncStringRepo: KotlinBase, AsyncRepo, @unchecked Sendable {
    var stored: String
    var invocations: Int = 0

    init(_ initial: String) {
        self.stored = initial
        super.init()
    }

    func fetch() async throws -> String {
        try await Task.sleep(nanoseconds: 1_000_000)
        invocations += 1
        return stored
    }

    func put(item: String) async throws -> Bool {
        try await Task.sleep(nanoseconds: 1_000_000)
        invocations += 1
        stored = item
        return true
    }

    func fetchOrFail(fail: Bool) async throws -> String {
        try await Task.sleep(nanoseconds: 1_000_000)
        invocations += 1
        if fail {
            throw SwiftRepoError(code: 503, label: "string-repo-down")
        }
        return stored
    }
}

private final class SwiftAsyncBoxRepo<T: _KotlinBridgeable>: KotlinBase, AsyncRepo, @unchecked Sendable {
    var box: Box<T>
    var failCode: Int

    init(_ initial: T, failCode: Int = 504) {
        self.box = Box(value: initial)
        self.failCode = failCode
        super.init()
    }

    func fetch() async throws -> T {
        try await Task.sleep(nanoseconds: 1_000_000)
        return box.value
    }

    func put(item: T) async throws -> Bool {
        try await Task.sleep(nanoseconds: 1_000_000)
        box.value = item
        return true
    }

    func fetchOrFail(fail: Bool) async throws -> T {
        try await Task.sleep(nanoseconds: 1_000_000)
        if fail {
            throw SwiftRepoError(code: failCode, label: "box-repo-\(failCode)")
        }
        return box.value
    }
}

private final class SwiftDelegatingAsyncRepo<T: _KotlinBridgeable>: KotlinBase, AsyncRepo, @unchecked Sendable {
    let upstream: any AsyncRepo<T>
    init(_ upstream: any AsyncRepo<T>) {
        self.upstream = upstream
        super.init()
    }
    func fetch() async throws -> T { try await upstream.fetch() }
    func put(item: T) async throws -> Bool { try await upstream.put(item: item) }
    func fetchOrFail(fail: Bool) async throws -> T { try await upstream.fetchOrFail(fail: fail) }
}

private func runAsyncLeakBatch(
    repo: SwiftAsyncBoxRepo<Int32>,
    flow: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Int32?>,
    iterations: Int32
) async throws {
    for i: Int32 in 0..<iterations {
        let pair = try await roundTripViaAsyncRepo(repo: repo, next: i)
        #expect(pair.second == i)

        let mapped = try await asyncMapBox(b: Box(value: i)) { v in
            "v:\(v)"
        }
        #expect(mapped.value == "v:\(i)")

        if i % 2 == 0 {
            try await flow.emit(value: i)
            #expect(flow.value == i)
        } else {
            try await flow.emit(value: nil)
            #expect(flow.value == nil)
        }

        if i % 20 == 0 {
            var count = 0
            for try await itemRepo in streamOfIntRepos(swiftRepo: repo).asAsyncSequence() {
                _ = try await itemRepo.fetch()
                count += 1
            }
            #expect(count == 3)
        }
    }
    try await flow.emit(value: nil)
}

@Suite(.serialized)
struct H3AsyncPatAndFlowsTests {
@Test
func crossLanguageSuspendGenericPatInterfacesInSwiftAndKotlin() async throws {
    // 1. Concrete SwiftAsyncStringRepo invoked from Kotlin suspend functions
    let strRepo = SwiftAsyncStringRepo("seed")
    let strPair = try await roundTripViaAsyncRepo(repo: strRepo, next: "updated-async")
    #expect(strPair.first == "seed")
    #expect(strPair.second == "updated-async")
    #expect(strRepo.stored == "updated-async")
    #expect(strRepo.invocations == 3)

    // 2. Generic SwiftAsyncBoxRepo<T> across Int32, String, Dog, and Int32? (nil <-> 0 <-> 42)
    let intRepo = SwiftAsyncBoxRepo<Int32>(10)
    let intPair = try await roundTripViaAsyncRepo(repo: intRepo, next: 99)
    #expect(intPair.first == 10)
    #expect(intPair.second == 99)
    #expect(intRepo.box.value == 99)

    let genericStrRepo = SwiftAsyncBoxRepo<String>("alpha")
    let genericStrPair = try await roundTripViaAsyncRepo(repo: genericStrRepo, next: "omega")
    #expect(genericStrPair.first == "alpha")
    #expect(genericStrPair.second == "omega")

    let dogRepo = SwiftAsyncBoxRepo<Dog>(Dog(name: "Rex", breed: "Shepherd"))
    let dogPair = try await roundTripViaAsyncRepo(
        repo: dogRepo,
        next: Dog(name: "Bolt", breed: "Husky")
    )
    #expect(dogPair.first.breed == "Shepherd")
    #expect(dogPair.second.name == "Bolt")
    #expect(dogPair.second.breed == "Husky")

    let nullableIntRepo = SwiftAsyncBoxRepo<Int32?>(nil)
    let p1 = try await roundTripViaAsyncRepo(repo: nullableIntRepo, next: 0)
    #expect(p1.first == nil)
    #expect(p1.second == 0)
    let p2 = try await roundTripViaAsyncRepo(repo: nullableIntRepo, next: 42)
    #expect(p2.first == 0)
    #expect(p2.second == 42)
    let p3 = try await roundTripViaAsyncRepo(repo: nullableIntRepo, next: nil)
    #expect(p3.first == 42)
    #expect(p3.second == nil)

    // 3. Error propagation: Swift -> Kotlin catch, Swift -> Kotlin -> Swift round-trip, and Kotlin -> Swift
    #expect(try await invokeFetchOrFailFromKotlin(repo: strRepo, fail: false) == "updated-async")
    #expect(try await catchFetchOrFailInKotlin(repo: strRepo, fail: false) == "ok:updated-async")
    let caughtInKt = try await catchFetchOrFailInKotlin(repo: strRepo, fail: true)
    #expect(caughtInKt.hasPrefix("caught:"))

    // Concrete SwiftRepoError thrown by SwiftAsyncStringRepo survives Swift -> Kotlin -> Swift intact
    let strFailResult = await Task<String, any Error>.detached {
        try await invokeFetchOrFailFromKotlin(repo: strRepo, fail: true)
    }.result
    if case .failure(let err) = strFailResult {
        #expect(err as? SwiftRepoError == SwiftRepoError(code: 503, label: "string-repo-down"))
    } else {
        Issue.record("Expected SwiftRepoError from SwiftAsyncStringRepo")
    }

    // Generic SwiftAsyncBoxRepo<Dog> error round-trip Swift -> Kotlin -> Swift
    let dogFailResult = await Task<Dog, any Error>.detached {
        try await invokeFetchOrFailFromKotlin(repo: SwiftAsyncBoxRepo<Dog>(Dog(name: "K9", breed: "Collie"), failCode: 507), fail: true)
    }.result
    if case .failure(let err) = dogFailResult {
        #expect(err as? SwiftRepoError == SwiftRepoError(code: 507, label: "box-repo-507"))
    } else {
        Issue.record("Expected SwiftRepoError from SwiftAsyncBoxRepo<Dog>")
    }

    // Exported KotlinAsyncRepo<Int32> & HiddenAsyncRepo<String> throw concrete AsyncRepoException to Swift
    let ktRepo = KotlinAsyncRepo<Int32>(item: 7, failCode: 418)
    #expect(try await ktRepo.fetchOrFail(fail: false) == 7)
    do {
        _ = try await ktRepo.fetchOrFail(fail: true)
        Issue.record("Expected AsyncRepoException")
    } catch let err as AsyncRepoException {
        #expect(err.errorCode == 418)
        #expect(err.message == "kotlin-repo-fail-418")
    }

    let hiddenRepo: any AsyncRepo<String> = makeHiddenAsyncRepo(initial: "secret")
    #expect(try await hiddenRepo.fetch() == "secret")
    do {
        _ = try await hiddenRepo.fetchOrFail(fail: true)
        Issue.record("Expected AsyncRepoException from HiddenAsyncRepo")
    } catch let err as AsyncRepoException {
        #expect(err.errorCode == 500)
    }

    // Pass-through Kotlin -> Swift(SwiftDelegatingAsyncRepo) -> Kotlin -> Swift preserves AsyncRepoException
    let delegating = SwiftDelegatingAsyncRepo<Int32>(ktRepo)
    let passThroughResult = await Task<Int32, any Error>.detached {
        try await invokeFetchOrFailFromKotlin(repo: delegating, fail: true)
    }.result
    if case .failure(let err) = passThroughResult {
        #expect((err as? AsyncRepoException)?.errorCode == 418)
    } else {
        Issue.record("Expected AsyncRepoException to survive pass-through")
    }
}

@Test
func kmpViewModelStateFlowSharedFlowRepoFlowsAndSuspendHigherOrder() async throws {
    // 1. StateFlow<UiState<User>> & MutableStateFlow<UiState<User>> observed via AsyncSequence
    //    while mutated from both Kotlin and Swift
    let vm = makeUserViewModel()
    let iterator = vm.state.asAsyncSequence().makeAsyncIterator()

    // Initial state is UiState.Loading (once next() returns, collector is actively subscribed)
    let firstState = try #require(try await iterator.next())
    switch firstState.sealedType() {
    case .loading:
        break
    default:
        Issue.record("Expected initial UiState.Loading")
    }

    // Mutate from Kotlin suspend method
    try await vm.transitionToReady(data: User(id: 1, handle: "alice"))
    let secondState = try #require(try await iterator.next())
    switch secondState.sealedType() {
    case .ready(let r):
        #expect(r.value.data.id == 1)
        #expect(r.value.data.handle == "alice")
    default:
        Issue.record("Expected UiState.Ready(alice)")
    }

    // Mutate from Kotlin synchronous setter
    vm.transitionToError(reason: "network-timeout")
    let thirdState = try #require(try await iterator.next())
    switch thirdState.sealedType() {
    case .error(let e):
        #expect(e.value.reason == "network-timeout")
    default:
        Issue.record("Expected UiState.Error")
    }

    // Mutate from Swift via MutableStateFlow<UiState<User>> (.emit, .value, and .compareAndSet)
    let bobReady: UiState<User> = UiState<User>.Ready(data: User(id: 2, handle: "bob"))
    try await vm.mutableState.emit(value: bobReady)
    let fourthState = try #require(try await iterator.next())
    switch fourthState.sealedType() {
    case .ready(let r):
        #expect(r.value.data.id == 2)
        #expect(r.value.data.handle == "bob")
    default:
        Issue.record("Expected UiState.Ready(bob)")
    }

    let carolReady: UiState<User> = UiState<User>.Ready(data: User(id: 3, handle: "carol"))
    #expect(vm.mutableState.compareAndSet(expect: bobReady, update: carolReady) == true)
    let fifthState = try #require(try await iterator.next())
    switch fifthState.sealedType() {
    case .ready(let r):
        #expect(r.value.data.handle == "carol")
    default:
        Issue.record("Expected UiState.Ready(carol)")
    }
    switch vm.state.value.sealedType() {
    case .ready(let r):
        #expect(r.value.data.id == 3)
    default:
        Issue.record("Expected state.value == carol")
    }

    // 2. MutableStateFlow<Int32?> (nil <-> 0 <-> 42 <-> nil transitions via AsyncSequence & .value)
    let nullableFlow = makeNullableIntStateFlow(initial: nil)
    let optIter = nullableFlow.asAsyncSequence().makeAsyncIterator()

    let initOpt = try await optIter.next()
    #expect(initOpt == .some(nil))
    #expect(nullableFlow.value == nil)

    try await nullableFlow.emit(value: 0)
    #expect(try await optIter.next() == .some(0))
    #expect(nullableFlow.value == 0)

    nullableFlow.value = 42
    #expect(try await optIter.next() == .some(42))
    #expect(nullableFlow.value == 42)

    #expect(nullableFlow.compareAndSet(expect: 42, update: nil) == true)
    #expect(try await optIter.next() == .some(nil))
    #expect(nullableFlow.value == nil)

    // 3. SharedFlow<Box<String>> / MutableSharedFlow<Box<String>>
    let bus = BoxEventBus<String>(replay: 2)
    try await bus.publishFromKotlin(item: "boot-1")
    try await bus.publishFromKotlin(item: "boot-2")
    #expect(bus.events.replayCache.map(\.value) == ["boot-1", "boot-2"])

    let collectTask = Task<[String], any Error>.detached {
        var collected: [String] = []
        for try await box in bus.events.asAsyncSequence() {
            collected.append(box.value)
            if collected.count == 4 { break }
        }
        return collected
    }

    while bus.mutableEvents.subscriptionCount.value < 1 {
        try await Task.sleep(nanoseconds: 1_000_000)
    }
    try await bus.publishFromKotlin(item: "live-kt")
    try await bus.mutableEvents.emit(value: Box(value: "live-swift"))

    let collectedEvents = try await collectTask.value
    #expect(collectedEvents == ["boot-1", "boot-2", "live-kt", "live-swift"])
    #expect(bus.events.replayCache.map(\.value) == ["live-kt", "live-swift"])

    // 4. Flow<any AsyncRepo<Int32>> emitting hidden Kotlin, exported Kotlin, and Swift PAT conformers
    let swiftStreamRepo = SwiftAsyncBoxRepo<Int32>(30)
    var fetchedBefore: [Int32] = []
    var fetchedAfter: [Int32] = []
    for try await repo in streamOfIntRepos(swiftRepo: swiftStreamRepo).asAsyncSequence() {
        let current = try await repo.fetch()
        fetchedBefore.append(current)
        let ok = try await repo.put(item: current + 100)
        #expect(ok)
        fetchedAfter.append(try await repo.fetch())
    }
    #expect(fetchedBefore == [10, 20, 30])
    #expect(fetchedAfter == [110, 120, 130])
    #expect(swiftStreamRepo.box.value == 130)

    // 5. Generic suspend higher-order functions asyncMapBox & asyncChainBoxes
    let mappedBox = try await asyncMapBox(b: Box(value: Int32(21))) { value in
        try await Task.sleep(nanoseconds: 1_000_000)
        return "mapped-\(value * 2)"
    }
    #expect(mappedBox.value == "mapped-42")

    let chainedBox = try await asyncChainBoxes(
        b: Box(value: Dog(name: "Luna", breed: "Samoyed")),
        first: { dog in
            try await Task.sleep(nanoseconds: 500_000)
            return "\(dog.name):\(dog.breed)"
        },
        second: { summary in
            try await Task.sleep(nanoseconds: 500_000)
            return Int32(summary.count)
        }
    )
    #expect(chainedBox.value == 12)

    // Throwing Swift async closure inside asyncMapBox propagates exact SwiftRepoError
    let mapThrowResult = await Task<Box<String>, any Error>.detached {
        try await asyncMapBox(b: Box(value: Int32(5))) { v -> String in
            try await Task.sleep(nanoseconds: 500_000)
            throw SwiftRepoError(code: Int(v), label: "closure-throw")
        }
    }.result
    if case .failure(let err) = mapThrowResult {
        #expect(err as? SwiftRepoError == SwiftRepoError(code: 5, label: "closure-throw"))
    } else {
        Issue.record("Expected SwiftRepoError from asyncMapBox closure")
    }
}

@Test
func zeroStableRefLeaksOverAsyncPatAndFlowLoops() async throws {
    let warmRepo = SwiftAsyncBoxRepo<Int32>(1)
    let warmFlow = makeNullableIntStateFlow(initial: nil)
    try await runAsyncLeakBatch(repo: warmRepo, flow: warmFlow, iterations: 4)
    try await Task.sleep(nanoseconds: 20_000_000)

    let beforeRefs = currentStableRefs()
    try #require(beforeRefs >= 0)

    try await runAsyncLeakBatch(repo: warmRepo, flow: warmFlow, iterations: 100)
    try await Task.sleep(nanoseconds: 20_000_000)

    let afterRefs = currentStableRefs()
    #expect(afterRefs - beforeRefs == 0, "Leaked \(afterRefs - beforeRefs) StableRefs over async loops!")
}
}
