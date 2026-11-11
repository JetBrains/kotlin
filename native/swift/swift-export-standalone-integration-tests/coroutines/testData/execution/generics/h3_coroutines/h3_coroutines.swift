import H3Coroutines
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import Testing

@Test
func genericSuspendAndHigherOrderAsync() async throws {
    let echoedInt = try await asyncEcho(x: Int32(42))
    #expect(echoedInt == 42)

    let echoedBox = try await asyncEcho(x: AsyncBox(value: "hello"))
    #expect(echoedBox.value == "hello")

    let mapped = try await asyncMap(x: Int32(21)) { v in
        "val:\(v * 2)"
    }
    #expect(mapped == "val:42")
}

@Test
func genericFlowsOfBoxesAndPatRepos() async throws {
    var boxValues: [Int32] = []
    for try await box in flowOfBoxes(a: Int32(10), b: Int32(20)).asAsyncSequence() {
        boxValues.append(box.value)
    }
    #expect(boxValues == [10, 20])

    var repoValues: [String] = []
    for try await repo in flowOfRepos(a: "first", b: "second").asAsyncSequence() {
        repoValues.append(repo.current())
    }
    #expect(repoValues == ["first", "second"])
}

@Test
func mutableStateFlowNullableElementEmit() async throws {
    let state = nullableIntStateFlow(initial: 5)
    #expect(state.value == 5)
    try await state.emit(value: nil)
    #expect(state.value == nil)
    try await state.emit(value: 19)
    #expect(state.value == 19)
}
