import KotlinRuntime
import SealedSuspend
import Testing

private func describe(_ state: LoadState) -> String {
    switch state.sealedType() {
        case .loading: "loading"
        case let .success(type): "success: \(type.value.data)"
        case let .failure(type): "failure: \(type.value.reason)"
    }
}

@Test
func testSuspendFunctionReturningASealedTypeIsMatchable() async throws {
    let success = try await loadSuccess()
    try #require(describe(success) == "success: loaded")
    let failure = try await loadFailure()
    try #require(describe(failure) == "failure: boom")
}

@Test
func testSuspendFunctionReturningANullableSealedType() async throws {
    let empty = try await loadNullable(empty: true)
    try #require(empty == nil)
    let value = try #require(try await loadNullable(empty: false))
    try #require(describe(value) == "loading")
}

@Test
func testSealedTypeAsSuspendParameter() async throws {
    let consumed = try await consume(state: LoadState.Success(data: "payload"))
    try #require(consumed == "consumed:Success(data=payload)")
}
