import UnboundGenerics
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinCoroutineSupport
import Testing

// KT-80260: values travelling through `Any` / an unbound type parameter (`any _KotlinBridgeable` in Swift).

@Test
func flowOfAnyElements() async throws {
    var xs: [Any] = []
    for try await e in flowOfAny().asAsyncSequence() { xs.append(e) }
    try #require(xs.count == 3)
    #expect(xs[0] as? String == "a")
    #expect(xs[1] as? Int32 == 1)
    #expect(xs[2] as? [Int32] == [2])
}

@Test
func flowOfT() async throws {
    var xs: [String?] = []
    for try await e in flowOfRepeatedT(x: "t").asAsyncSequence() { xs.append(e as? String) }
    #expect(xs == ["t", "t"])
}

@Test
func typedCollectionElements() async throws {
    var lists: [[String]] = []
    for try await e in flowListStr().asAsyncSequence() { lists.append(e) }
    #expect(lists == [["a", "b"]])
    var sets: [Set<String>] = []
    for try await e in flowOfStringSet().asAsyncSequence() { sets.append(e) }
    #expect(sets == [["a", "b"]])
    var maps: [[String: Int32]] = []
    for try await e in flowOfStringToIntMap().asAsyncSequence() { maps.append(e) }
    #expect(maps == [["a": 1]])
}

@Test
func subclassElementOfSealedFlow() async throws {
    var ids: [Int32?] = []
    for try await e in flowSealed().asAsyncSequence() { ids.append((e as? SealedLeaf)?.id) }
    #expect(ids == [3])
}

@Test
func emptyFlowOfNothing() async throws {
    var count = 0
    for try await _ in flowNothing().asAsyncSequence() { count += 1 }
    #expect(count == 0)
}

@Test
func stateFlowValueAndReplayCache() async throws {
    setState(value: "init")
    #expect(stateFlow().value == "init")
    setState(value: "next")
    #expect(stateFlow().value == "next")
    #expect(stateFlow().replayCache == ["next"])
    #expect(stateNothing().value == nil)
}
